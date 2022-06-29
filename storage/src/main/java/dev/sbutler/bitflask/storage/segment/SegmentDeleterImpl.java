package dev.sbutler.bitflask.storage.segment;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.assistedinject.Assisted;
import dev.sbutler.bitflask.storage.configuration.concurrency.StorageExecutorService;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import javax.inject.Inject;

class SegmentDeleterImpl implements SegmentDeleter {

  private final ExecutorService executorService;
  private final ImmutableList<Segment> segmentsToBeDeleted;
  private final List<Consumer<DeletionResults>> deletionResultsConsumers = new CopyOnWriteArrayList<>();
  private volatile boolean deletionStarted = false;

  @Inject
  SegmentDeleterImpl(@StorageExecutorService ExecutorService executorService,
      @Assisted ImmutableList<Segment> segmentsToBeDeleted) {
    this.executorService = executorService;
    this.segmentsToBeDeleted = segmentsToBeDeleted;
  }

  @Override
  public void deleteSegments() {
    if (deletionStarted) {
      return;
    }

    deletionStarted = true;
    CompletableFuture.
        supplyAsync(this::closeAndDeleteSegments, executorService)
        .whenCompleteAsync(this::handleCloseAndDeleteSegmentsOutcome, executorService)
        .thenApplyAsync(this::mapPotentialSegmentFailures, executorService)
        .thenAcceptAsync(this::handlePotentialSegmentFailuresOutcome, executorService);

  }

  @Override
  public void registerDeletionResultsConsumer(Consumer<DeletionResults> deletionResultsConsumer) {
    deletionResultsConsumers.add(deletionResultsConsumer);
  }

  private void runRegisteredDeletionResultsConsumers(DeletionResults deletionResults) {
    deletionResultsConsumers.forEach(consumer -> consumer.accept(deletionResults));
  }

  /**
   * Initiates asynchronous closing and deleting of segments.
   *
   * @return a map of segments to their corresponding deletion futures
   */
  private ImmutableMap<Segment, Future<Void>> closeAndDeleteSegments() {
    ImmutableList<Future<Void>> deletionFutures;
    try {
      deletionFutures = ImmutableList.copyOf(executorService.invokeAll(
          segmentsToBeDeleted.stream()
              .map(segment -> (Callable<Void>) () -> {
                segment.close();
                segment.delete();
                return null;
              }).collect(toImmutableList())
      ));
    } catch (InterruptedException e) {
      throw new CompletionException(e);
    }

    ImmutableMap.Builder<Segment, Future<Void>> segmentDeletionFutureMap = ImmutableMap.builder();
    for (int i = 0; i < segmentsToBeDeleted.size(); i++) {
      segmentDeletionFutureMap.put(segmentsToBeDeleted.get(i), deletionFutures.get(i));
    }
    return segmentDeletionFutureMap.build();
  }

  private void handleCloseAndDeleteSegmentsOutcome(
      ImmutableMap<Segment, Future<Void>> segmentFutureResults,
      Throwable throwable) {
    if (throwable != null) {
      runRegisteredDeletionResultsConsumers(
          new DeletionResultsImpl(segmentsToBeDeleted, throwable.getCause()));
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
      } catch (InterruptedException e) {
        segmentFailuresMap.put(segment, e);
      } catch (ExecutionException e) {
        segmentFailuresMap.put(segment, e.getCause());
      }
    });
    return segmentFailuresMap.build();
  }

  private void handlePotentialSegmentFailuresOutcome(
      ImmutableMap<Segment, Throwable> potentialSegmentFailures) {
    DeletionResultsImpl deletionResults;
    if (potentialSegmentFailures.isEmpty()) {
      deletionResults = new DeletionResultsImpl(segmentsToBeDeleted);
    } else {
      deletionResults = new DeletionResultsImpl(segmentsToBeDeleted, potentialSegmentFailures);
    }
    runRegisteredDeletionResultsConsumers(deletionResults);
  }

  private static class DeletionResultsImpl implements DeletionResults {

    private final Status status;
    private final ImmutableList<Segment> segmentsProvidedForDeletion;
    private Throwable generalFailureReason = null;
    private ImmutableMap<Segment, Throwable> segmentsFailureReasonsMap = null;

    DeletionResultsImpl(ImmutableList<Segment> segmentsProvidedForDeletion) {
      this.status = Status.SUCCESS;
      this.segmentsProvidedForDeletion = segmentsProvidedForDeletion;
    }

    DeletionResultsImpl(ImmutableList<Segment> segmentsProvidedForDeletion, Throwable throwable) {
      this.status = Status.FAILED_GENERAL;
      this.segmentsProvidedForDeletion = segmentsProvidedForDeletion;
      this.generalFailureReason = throwable;
    }

    DeletionResultsImpl(ImmutableList<Segment> segmentsProvidedForDeletion,
        ImmutableMap<Segment, Throwable> segmentsFailureReasonsMap) {
      this.status = Status.FAILED_SEGMENTS;
      this.segmentsProvidedForDeletion = segmentsProvidedForDeletion;
      this.segmentsFailureReasonsMap = segmentsFailureReasonsMap;
    }

    @Override
    public Status getStatus() {
      return status;
    }

    @Override
    public ImmutableList<Segment> getSegmentsProvidedForDeletion() {
      return segmentsProvidedForDeletion;
    }

    @Override
    public Throwable getGeneralFailureReason() {
      return generalFailureReason;
    }

    @Override
    public ImmutableMap<Segment, Throwable> getSegmentsFailureReasonsMap() {
      return segmentsFailureReasonsMap;
    }
  }

}
