package dev.sbutler.bitflask.storage.segmentV1;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import dev.sbutler.bitflask.storage.segmentV1.SegmentDeleter.DeletionResults.FailedGeneral;
import dev.sbutler.bitflask.storage.segmentV1.SegmentDeleter.DeletionResults.FailedSegments;
import dev.sbutler.bitflask.storage.segmentV1.SegmentDeleter.DeletionResults.Success;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import jdk.incubator.concurrent.StructuredTaskScope;

/**
 * Handling the process of closing and deleting multiple Segments from the file system.
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

    private final ThreadFactory threadFactory;

    @Inject
    Factory(ThreadFactory threadFactory) {
      this.threadFactory = threadFactory;
    }

    /**
     * Creates a SegmentDeleter for deleting the provided segments.
     *
     * @param segmentsToBeDeleted the segments to be deleted by the SegmentDeleter
     * @return the created SegmentDeleter
     */
    SegmentDeleter create(ImmutableList<Segment> segmentsToBeDeleted) {
      return new SegmentDeleter(threadFactory, segmentsToBeDeleted);
    }
  }

  private final ThreadFactory threadFactory;
  private final ImmutableList<Segment> segmentsToBeDeleted;

  private final AtomicBoolean deletionStarted = new AtomicBoolean();

  private SegmentDeleter(ThreadFactory threadFactory,
      ImmutableList<Segment> segmentsToBeDeleted) {
    this.threadFactory = threadFactory;
    this.segmentsToBeDeleted = segmentsToBeDeleted;
  }

  /**
   * Starts the deletion process for all Segments provided.
   *
   * <p>This is a blocking call.
   *
   * <p>The deletion process can only be started once. Subsequent calls will result in an
   * IllegalStateException being thrown.
   *
   * <p>Anticipated exceptions thrown while closing and deleting an exception will
   * be captured and provided in the returned DeletionResults.
   */
  public DeletionResults deleteSegments() {
    if (deletionStarted.getAndSet(true)) {
      throw new IllegalStateException("Deletion has already been started");
    }

    ImmutableMap<Segment, Future<Void>> segmentCloseAndDeleteFutures;
    try {
      segmentCloseAndDeleteFutures = closeAndDeleteSegments();
    } catch (Exception e) {
      return new FailedGeneral(segmentsToBeDeleted, e);
    }

    ImmutableMap<Segment, Throwable> segmentFailuresMap =
        mapPotentialSegmentFailures(segmentCloseAndDeleteFutures);
    return handlePotentialSegmentFailuresOutcome(segmentFailuresMap);
  }

  /**
   * Initiates asynchronous closing and deleting of segments.
   *
   * @return a map of segments to their corresponding deletion futures
   */
  private ImmutableMap<Segment, Future<Void>> closeAndDeleteSegments() throws Exception {
    try (var scope = new StructuredTaskScope<>("deletion-close-delete-scope",
        threadFactory)) {
      ImmutableMap.Builder<Segment, Future<Void>> segmentDeletionFutureMap = ImmutableMap.builder();
      for (Segment segment : segmentsToBeDeleted) {
        Future<Void> closeAndDeleteFuture = scope.fork(() -> {
          segment.close();
          segment.deleteSegment();
          return null;
        });
        segmentDeletionFutureMap.put(segment, closeAndDeleteFuture);
      }
      scope.join();
      return segmentDeletionFutureMap.build();
    }
  }

  /**
   * Checks the result of each Segment's deletion and creates a map from the segment to its reason
   * for failure, if its deletion failed.
   *
   * @param segmentDeletionFutures a map of segments to their corresponding deletion future
   * @return a map of segments which failed to be deleted and their reason for failure
   */
  private ImmutableMap<Segment, Throwable> mapPotentialSegmentFailures(
      ImmutableMap<Segment, Future<Void>> segmentDeletionFutures) {
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

  /**
   * Processes the segment failure map creating the {@link DeletionResults} for this execution.
   *
   * @param potentialSegmentFailures a map of failed segments to their reason for failure.
   * @return {@link Success} will be returned if there were no failures. A {@link FailedSegments} if
   * some segments failed to be closed and deleted.
   */
  private DeletionResults handlePotentialSegmentFailuresOutcome(
      ImmutableMap<Segment, Throwable> potentialSegmentFailures) {
    if (potentialSegmentFailures.isEmpty()) {
      return new Success(segmentsToBeDeleted);
    } else {
      return new FailedSegments(segmentsToBeDeleted, potentialSegmentFailures);
    }
  }
}
