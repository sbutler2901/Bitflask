package dev.sbutler.bitflask.storage.segment;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.storage.segment.SegmentDeleter.DeletionResults.FailedGeneral;
import dev.sbutler.bitflask.storage.segment.SegmentDeleter.DeletionResults.FailedSegments;
import dev.sbutler.bitflask.storage.segment.SegmentDeleter.DeletionResults.Success;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;

/**
 * Asynchronously deletes multiple segments Handles the process of deleting multiple Segments from
 * the file system.
 */
final class SegmentDeleter {

  /**
   * Relays the results of a deletion execution
   */
  sealed interface DeletionResults {

    record Success(ImmutableList<Segment> segmentsProvidedForDeletion)
        implements DeletionResults {

    }

    /**
     * Contains the result of failed deletion execution caused by a general failure
     */
    record FailedGeneral(ImmutableList<Segment> segmentsProvidedForDeletion,
                         Throwable failureReason) implements DeletionResults {

    }

    /**
     * Contains the result of failed deletion execution caused by specific segment(s)
     */
    record FailedSegments(ImmutableList<Segment> segmentsProvidedForDeletion,
                          ImmutableMap<Segment, Throwable> segmentsFailureReasonsMap)
        implements DeletionResults {

    }
  }

  /**
   * Factory for creating new SegmentDeleter instances.
   */
  static class Factory {

    private final ListeningExecutorService executorService;

    @Inject
    Factory(ListeningExecutorService executorService) {
      this.executorService = executorService;
    }

    /**
     * Creates a SegmentDeleter for deleting the provided segments.
     *
     * @param segmentsToBeDeleted the segments to be deleted by the SegmentDeleter
     * @return the created SegmentDeleter
     */
    SegmentDeleter create(ImmutableList<Segment> segmentsToBeDeleted) {
      return new SegmentDeleter(executorService, segmentsToBeDeleted);
    }
  }

  private final ListeningExecutorService executorService;
  private final ImmutableList<Segment> segmentsToBeDeleted;
  private ListenableFuture<DeletionResults> deletionFuture = null;

  private SegmentDeleter(ListeningExecutorService executorService,
      ImmutableList<Segment> segmentsToBeDeleted) {
    this.executorService = executorService;
    this.segmentsToBeDeleted = segmentsToBeDeleted;
  }

  /**
   * Starts the deletion process for all Segments provided.
   *
   * <p>The deletion process can only be started once. After the initial call, subsequent calls
   * will return the same ListenableFuture as the initial.
   *
   * <p>Any exceptions thrown during execution will be captured and provided in the returned
   * DeletionResults.
   *
   * @return a Future that will be fulfilled with the results of deletion, whether success or
   * failure
   */
  @SuppressWarnings("UnstableApiUsage")
  public synchronized ListenableFuture<DeletionResults> deleteSegments() {
    if (deletionFuture != null) {
      return deletionFuture;
    }

    return deletionFuture =
        FluentFuture.from(Futures.submit(this::closeAndDeleteSegments, executorService))
            .transform(this::mapPotentialSegmentFailures, executorService)
            .transform(this::handlePotentialSegmentFailuresOutcome, executorService)
            .catching(Throwable.class, this::catchDeletionFailure, executorService);
  }

  /**
   * Initiates asynchronous closing and deleting of segments.
   *
   * @return a map of segments to their corresponding deletion futures
   */
  private ImmutableMap<Segment, ListenableFuture<Void>> closeAndDeleteSegments()
      throws InterruptedException {
    ImmutableList.Builder<Callable<Void>> deletionCallables = ImmutableList.builder();
    for (Segment segment : segmentsToBeDeleted) {
      deletionCallables.add(() -> {
        segment.close();
        segment.delete();
        return null;
      });
    }

    @SuppressWarnings({"unchecked", "rawtypes"}) // guaranteed by invokeAll contract
    List<ListenableFuture<Void>> deletionFutures =
        (List) executorService.invokeAll(deletionCallables.build());

    ImmutableMap.Builder<Segment, ListenableFuture<Void>> segmentDeletionFutureMap = ImmutableMap.builder();
    for (int i = 0; i < segmentsToBeDeleted.size(); i++) {
      segmentDeletionFutureMap.put(segmentsToBeDeleted.get(i), deletionFutures.get(i));
    }
    return segmentDeletionFutureMap.build();
  }

  /**
   * Checks the result of each Segment's deletion and creates a map from the segment to its reason
   * for failure, if its deletion failed.
   *
   * @param segmentDeletionFutures a map of segments to their corresponding deletion future
   * @return a map of segments which failed to be deleted and their reason for failure
   */
  private ImmutableMap<Segment, Throwable> mapPotentialSegmentFailures(
      ImmutableMap<Segment, ListenableFuture<Void>> segmentDeletionFutures) {
    ImmutableMap.Builder<Segment, Throwable> segmentFailuresMap = ImmutableMap.builder();
    segmentDeletionFutures.forEach((segment, deletionFuture) -> {
      try {
        deletionFuture.get();
      } catch (ExecutionException | InterruptedException e) {
        segmentFailuresMap.put(segment, e);
      }
    });
    return segmentFailuresMap.build();
  }

  private DeletionResults handlePotentialSegmentFailuresOutcome(
      ImmutableMap<Segment, Throwable> potentialSegmentFailures) {
    if (potentialSegmentFailures.isEmpty()) {
      return new Success(segmentsToBeDeleted);
    } else {
      return new FailedSegments(segmentsToBeDeleted, potentialSegmentFailures);
    }
  }

  private DeletionResults catchDeletionFailure(Throwable throwable) {
    return new FailedGeneral(segmentsToBeDeleted, throwable);
  }
}
