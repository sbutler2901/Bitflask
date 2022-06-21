package dev.sbutler.bitflask.storage.segment;

import com.google.inject.assistedinject.Assisted;
import dev.sbutler.bitflask.storage.configuration.concurrency.StorageExecutorService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.inject.Inject;

class SegmentDeleterImpl implements SegmentDeleter {

  private final ExecutorService executorService;
  private final List<Segment> segmentsToBeDeleted;
  private final List<Consumer<DeletionResults>> deletionResultsConsumers = new ArrayList<>();
  private volatile boolean deletionStarted = false;

  @Inject
  SegmentDeleterImpl(@StorageExecutorService ExecutorService executorService,
      @Assisted List<Segment> segmentsToBeDeleted) {
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

  List<Future<Void>> closeAndDeleteSegments() {
    try {
      return executorService.invokeAll(
          segmentsToBeDeleted.stream().map(segment -> (Callable<Void>) () -> {
            segment.close();
            segment.delete();
            return null;
          }).collect(Collectors.toList())
      );
    } catch (InterruptedException e) {
      throw new CompletionException(e);
    }
  }

  void handleCloseAndDeleteSegmentsOutcome(List<Future<Void>> segmentFutureResults,
      Throwable throwable) {
    if (throwable != null) {
      runRegisteredDeletionResultsConsumers(
          new DeletionResultsImpl(segmentsToBeDeleted, throwable.getCause()));
    }
  }

  Map<Segment, Throwable> mapPotentialSegmentFailures(
      List<Future<Void>> segmentFutureResults) {
    Map<Segment, Throwable> segmentFailuresMap = new HashMap<>();
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
    return segmentFailuresMap;
  }

  void handlePotentialSegmentFailuresOutcome(Map<Segment, Throwable> potentialSegmentFailures) {
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
    private final List<Segment> segmentsToBeDeleted;
    private Throwable generalFailureReason = null;
    private Map<Segment, Throwable> segmentsFailureReasonsMap = new HashMap<>();

    DeletionResultsImpl(List<Segment> segmentsToBeDeleted) {
      this.status = Status.SUCCESS;
      this.segmentsToBeDeleted = segmentsToBeDeleted;
    }

    DeletionResultsImpl(List<Segment> segmentsToBeDeleted, Throwable throwable) {
      this.status = Status.FAILED_GENERAL;
      this.segmentsToBeDeleted = segmentsToBeDeleted;
      this.generalFailureReason = throwable;
    }

    DeletionResultsImpl(List<Segment> segmentsToBeDeleted,
        Map<Segment, Throwable> segmentsFailureReasonsMap) {
      this.status = Status.FAILED_SEGMENTS;
      this.segmentsToBeDeleted = segmentsToBeDeleted;
      this.segmentsFailureReasonsMap = segmentsFailureReasonsMap;
    }

    @Override
    public Status getStatus() {
      return status;
    }

    @Override
    public List<Segment> getSegmentsToBeDeleted() {
      return segmentsToBeDeleted;
    }

    @Override
    public Throwable getGeneralFailureReason() {
      return generalFailureReason;
    }

    @Override
    public Map<Segment, Throwable> getSegmentsFailureReasonsMap() {
      return segmentsFailureReasonsMap;
    }
  }

}
