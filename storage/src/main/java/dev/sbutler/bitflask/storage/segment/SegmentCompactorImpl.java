package dev.sbutler.bitflask.storage.segment;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import dev.sbutler.bitflask.storage.configuration.concurrency.StorageExecutorService;
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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

class SegmentCompactorImpl implements SegmentCompactor {

  private final ExecutorService executorService;
  private final SegmentFactory segmentFactory;
  private final ImmutableList<Segment> segmentsToBeCompacted;
  private final List<Consumer<CompactionCompletionResults>> compactionCompletedConsumers = new CopyOnWriteArrayList<>();
  private final List<BiConsumer<Throwable, ImmutableList<Segment>>> compactionFailedConsumers = new CopyOnWriteArrayList<>();
  private volatile boolean compactionStarted = false;

  private ImmutableList<Segment> failedCompactedSegments = ImmutableList.of();

  @Inject
  SegmentCompactorImpl(@StorageExecutorService ExecutorService executorService,
      SegmentFactory segmentFactory,
      @Assisted ImmutableList<Segment> segmentsToBeCompacted) {
    this.executorService = executorService;
    this.segmentFactory = segmentFactory;
    this.segmentsToBeCompacted = segmentsToBeCompacted;
  }

  @Override
  public void compactSegments() {
    if (compactionStarted) {
      return;
    }

    compactionStarted = true;
    CompletableFuture
        .supplyAsync(this::createKeySegmentMap, executorService)
        .thenApplyAsync(this::createCompactedSegments, executorService)
        .whenCompleteAsync(this::handleCompactionOutcome, executorService);
  }

  @Override
  public void registerCompactionCompletedConsumer(
      Consumer<CompactionCompletionResults> compactionCompletedConsumer) {
    compactionCompletedConsumers.add(compactionCompletedConsumer);
  }

  private void runRegisteredCompactionCompletedConsumers(
      CompactionCompletionResults compactionCompletionResults) {
    compactionCompletedConsumers.forEach(consumer -> consumer.accept(compactionCompletionResults));
  }

  @Override
  public void registerCompactionFailedConsumer(
      BiConsumer<Throwable, ImmutableList<Segment>> compactionFailedConsumer) {
    compactionFailedConsumers.add(compactionFailedConsumer);
  }

  private void runRegisteredCompactionFailedConsumers(Throwable throwable) {
    compactionFailedConsumers.forEach(
        consumer -> consumer.accept(throwable, failedCompactedSegments));
  }

  /**
   * Creates a map of keys to the segment with the most up-to-date value for key.
   *
   * @return a map of keys to the Segment from which its corresponding value should be read
   */
  private ImmutableMap<String, Segment> createKeySegmentMap() {
    Map<String, Segment> keySegmentMap = new HashMap<>();
    for (Segment segment : segmentsToBeCompacted) {
      Set<String> segmentKeys = segment.getSegmentKeys();
      for (String key : segmentKeys) {
        if (!keySegmentMap.containsKey(key)) {
          keySegmentMap.put(key, segment);
        }
      }
    }
    return ImmutableMap.copyOf(keySegmentMap);
  }

  /**
   * Creates compacted segments using the most up-to-date value for each key.
   *
   * @param keySegmentMap a map of keys to the Segment from which its corresponding value should be
   *                      read
   * @return all compacted segments created
   */
  private ImmutableList<Segment> createCompactedSegments(
      ImmutableMap<String, Segment> keySegmentMap) {
    List<Segment> compactedSegments = new ArrayList<>();
    try {
      compactedSegments.add(0, segmentFactory.createSegment());
      for (Map.Entry<String, Segment> entry : keySegmentMap.entrySet()) {
        String key = entry.getKey();
        Optional<String> valueOptional = entry.getValue().read(key);
        if (valueOptional.isEmpty()) {
          throw new RuntimeException("Compaction failure: value not found while reading segment");
        }

        if (compactedSegments.get(0).exceedsStorageThreshold()) {
          compactedSegments.add(0, segmentFactory.createSegment());
        }

        compactedSegments.get(0).write(key, valueOptional.get());
      }
    } catch (IOException e) {
      failedCompactedSegments = ImmutableList.copyOf(compactedSegments);
      throw new CompletionException(e);
    }

    return ImmutableList.copyOf(compactedSegments);
  }

  private void handleCompactionOutcome(ImmutableList<Segment> compactedSegments,
      Throwable throwable) {
    if (throwable != null) {
      runRegisteredCompactionFailedConsumers(throwable.getCause());
    } else {
      markSegmentsCompacted();
      runRegisteredCompactionCompletedConsumers(
          new CompactionCompletionResultsImpl(compactedSegments, segmentsToBeCompacted));
    }
  }

  private void markSegmentsCompacted() {
    for (Segment segment : segmentsToBeCompacted) {
      segment.markCompacted();
    }
  }

  private record CompactionCompletionResultsImpl(ImmutableList<Segment> compactedSegments,
                                                 ImmutableList<Segment> segmentsProvidedForCompaction) implements
      CompactionCompletionResults {

  }

}
