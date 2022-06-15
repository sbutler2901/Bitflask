package dev.sbutler.bitflask.storage.segment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * Handler compacting the provided segments into new segments only keeping the latest key:value
 * pairs.
 * <p>
 * Note: A copy of the provided preCompactedSegments will be made during construction.
 */
class SegmentCompactorImpl implements SegmentCompactor {

  private final ExecutorService executorService;
  private final SegmentFactory segmentFactory;
  private final List<Consumer<List<Segment>>> compactionResultsConsumers = new ArrayList<>();
  private final List<Runnable> compactionCompleteRunnables = new ArrayList<>();
  private final List<Consumer<Throwable>> compactionFailedConsumers = new ArrayList<>();
  private List<Segment> preCompactedSegments = null;

  SegmentCompactorImpl(ExecutorService executorService, SegmentFactory segmentFactory) {
    this.executorService = executorService;
    this.segmentFactory = segmentFactory;
  }

  @Override
  public void compactSegments() {
    preCompactionValidation();

    CompletableFuture
        .supplyAsync(this::createKeySegmentMap, executorService)
        .thenApplyAsync(this::createCompactedSegments, executorService)
        .whenCompleteAsync(this::handleCompactionOutcome, executorService)
        .thenAcceptAsync(this::runRegisteredCompactionResultsConsumers, executorService)
        .thenRunAsync(this::runRegisteredCompletionRunnables, executorService);
  }

  private void preCompactionValidation() {
    if (preCompactedSegments == null) {
      throw new RuntimeException(
          "Pre-compacted segments were not set prior to starting compaction");
    }
  }

  @Override
  public void setPreCompactedSegments(List<Segment> preCompactedSegments) {
    this.preCompactedSegments = List.copyOf(preCompactedSegments);
  }

  @Override
  public void registerCompactionResultsConsumer(Consumer<List<Segment>> compactionResultsConsumer) {
    compactionResultsConsumers.add(compactionResultsConsumer);
  }

  @Override
  public void registerCompactionCompletedRunnable(Runnable compactionCompletedRunnable) {
    compactionCompleteRunnables.add(compactionCompletedRunnable);
  }

  @Override
  public void registerCompactionFailedConsumer(Consumer<Throwable> compactionFailedConsumer) {
    compactionFailedConsumers.add(compactionFailedConsumer);
  }

  private void runRegisteredCompactionResultsConsumers(List<Segment> compactedSegments) {
    compactionResultsConsumers.forEach(consumer -> consumer.accept(compactedSegments));
  }

  private void runRegisteredCompletionRunnables() {
    compactionCompleteRunnables.forEach(Runnable::run);
  }

  private void runRegisteredCompactionFailedConsumers(Throwable throwable) {
    compactionFailedConsumers.forEach(consumer -> consumer.accept(throwable));
  }

  private void handleCompactionOutcome(List<Segment> results, Throwable throwable) {
    if (throwable != null) {
      runRegisteredCompactionFailedConsumers(throwable.getCause());
    } else {
      markSegmentsCompacted();
    }
  }

  private Map<String, Segment> createKeySegmentMap() {
    Map<String, Segment> keySegmentMap = new HashMap<>();
    for (Segment segment : preCompactedSegments) {
      Set<String> segmentKeys = segment.getSegmentKeys();
      for (String key : segmentKeys) {
        if (!keySegmentMap.containsKey(key)) {
          keySegmentMap.put(key, segment);
        }
      }
    }
    return keySegmentMap;
  }

  private List<Segment> createCompactedSegments(Map<String, Segment> keySegmentMap) {
    List<Segment> compactedSegments = new CopyOnWriteArrayList<>();

    try {
      Segment currentCompactedSegment = segmentFactory.createSegment();
      for (Map.Entry<String, Segment> entry : keySegmentMap.entrySet()) {
        String key = entry.getKey();
        Optional<String> valueOptional = entry.getValue().read(key);
        if (valueOptional.isEmpty()) {
          throw new RuntimeException("Compaction failure: value not found while reading segment");
        }

        currentCompactedSegment.write(key, valueOptional.get());
        if (currentCompactedSegment.exceedsStorageThreshold()) {
          compactedSegments.add(0, currentCompactedSegment);
          currentCompactedSegment = segmentFactory.createSegment();
        }
      }
      compactedSegments.add(0, currentCompactedSegment);
    } catch (IOException e) {
      throw new CompletionException(e);
    }

    return compactedSegments;
  }

  private void markSegmentsCompacted() {
    for (Segment segment : preCompactedSegments) {
      segment.markCompacted();
    }
  }

  public List<Segment> closeAndDeleteSegments() {
    List<Segment> failedSegments = new ArrayList<>();
    for (Segment segment : preCompactedSegments) {
      try {
        segment.closeAndDelete();
      } catch (IOException e) {
        failedSegments.add(segment);
      }
    }
    return failedSegments;
  }
}
