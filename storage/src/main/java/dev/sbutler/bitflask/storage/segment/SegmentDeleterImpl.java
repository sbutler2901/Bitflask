package dev.sbutler.bitflask.storage.segment;

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
import java.util.stream.Collectors;
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

  ImmutableList<Future<Void>> closeAndDeleteSegments() {
    try {
      return ImmutableList.copyOf(executorService.invokeAll(
          segmentsToBeDeleted.stream().map(segment -> (Callable<Void>) () -> {
            segment.close();
            segment.delete();
            return null;
          }).collect(Collectors.toList())
      ));
    } catch (InterruptedException e) {
      throw new CompletionException(e);
    }
  }

  void handleCloseAndDeleteSegmentsOutcome(ImmutableList<Future<Void>> segmentFutureResults,
      Throwable throwable) {
    if (throwable != null) {
      runRegisteredDeletionResultsConsumers(
          new DeletionResultsImpl(segmentsToBeDeleted, throwable.getCause()));
    }
  }

  ImmutableMap<Segment, Throwable> mapPotentialSegmentFailures(
      ImmutableList<Future<Void>> segmentFutureResults) {
    ImmutableMap.Builder<Segment, Throwable> segmentFailuresMap = ImmutableMap.builder();
    for (int i = 0; i < segmentsToBeDeleted.size(); i++) {
      Segment segment = segmentsToBeDeleted.get(i);
      Future<Void> segmentResult = segmentFutureResults.get(i);

      try {
        segmentResult.get();
      } catch (InterruptedException e) {
        segmentFailuresMap.put(segment, e);
      } catch (ExecutionException e) {
        segmentFailuresMap.put(segment, e.getCause());
      }
    }
    return segmentFailuresMap.build();
  }

  void handlePotentialSegmentFailuresOutcome(
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
    private final ImmutableList<Segment> segmentsToBeDeleted;
    private Throwable generalFailureReason = null;
    private ImmutableMap<Segment, Throwable> segmentsFailureReasonsMap;

    DeletionResultsImpl(ImmutableList<Segment> segmentsToBeDeleted) {
      this.status = Status.SUCCESS;
      this.segmentsToBeDeleted = segmentsToBeDeleted;
    }

    DeletionResultsImpl(ImmutableList<Segment> segmentsToBeDeleted, Throwable throwable) {
      this.status = Status.FAILED_GENERAL;
      this.segmentsToBeDeleted = segmentsToBeDeleted;
      this.generalFailureReason = throwable;
    }

    DeletionResultsImpl(ImmutableList<Segment> segmentsToBeDeleted,
        ImmutableMap<Segment, Throwable> segmentsFailureReasonsMap) {
      this.status = Status.FAILED_SEGMENTS;
      this.segmentsToBeDeleted = segmentsToBeDeleted;
      this.segmentsFailureReasonsMap = segmentsFailureReasonsMap;
    }

    @Override
    public Status getStatus() {
      return status;
    }

    @Override
    public ImmutableList<Segment> getSegmentsToBeDeleted() {
      return segmentsToBeDeleted;
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
