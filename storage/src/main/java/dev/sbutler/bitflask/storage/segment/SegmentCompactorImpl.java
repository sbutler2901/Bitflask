package dev.sbutler.bitflask.storage.segment;

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
import java.util.function.Consumer;

class SegmentCompactorImpl implements SegmentCompactor {

  private final ExecutorService executorService;
  private final SegmentFactory segmentFactory;
  private final List<Segment> preCompactedSegments;
  private final List<Consumer<CompactionCompletionResults>> compactionCompletedConsumers = new ArrayList<>();
  private final List<Consumer<Throwable>> compactionFailedConsumers = new ArrayList<>();
  private volatile boolean compactionStarted = false;

  @Inject
  SegmentCompactorImpl(@StorageExecutorService ExecutorService executorService,
      SegmentFactory segmentFactory,
      @Assisted List<Segment> preCompactedSegments) {
    this.executorService = executorService;
    this.segmentFactory = segmentFactory;
    this.preCompactedSegments = List.copyOf(preCompactedSegments);
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
  public void registerCompactionFailedConsumer(Consumer<Throwable> compactionFailedConsumer) {
    compactionFailedConsumers.add(compactionFailedConsumer);
  }

  private void runRegisteredCompactionFailedConsumers(Throwable throwable) {
    compactionFailedConsumers.forEach(consumer -> consumer.accept(throwable));
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

        if (currentCompactedSegment.exceedsStorageThreshold()) {
          compactedSegments.add(0, currentCompactedSegment);
          currentCompactedSegment = segmentFactory.createSegment();
        }

        currentCompactedSegment.write(key, valueOptional.get());
      }
      compactedSegments.add(0, currentCompactedSegment);
    } catch (IOException e) {
      throw new CompletionException(e);
    }

    return compactedSegments;
  }

  private void handleCompactionOutcome(List<Segment> compactedSegments, Throwable throwable) {
    if (throwable != null) {
      runRegisteredCompactionFailedConsumers(throwable.getCause());
    } else {
      markSegmentsCompacted();
      runRegisteredCompactionCompletedConsumers(
          new CompactionCompletionResultsImpl(compactedSegments, preCompactedSegments));
    }
  }

  private void markSegmentsCompacted() {
    for (Segment segment : preCompactedSegments) {
      segment.markCompacted();
    }
  }

  // todo: move to another class
//  private void closeAndDeleteSegments() {
//    try {
//      List<Future<Void>> closeAndDeleteResults = executorService.invokeAll(
//          preCompactedSegments.stream().map(segment -> (Callable<Void>) () -> {
//            // todo: separate
//            segment.close();
//            segment.delete();
//            return null;
//          }).toList()
//      );
//      for (int i = 0; i < preCompactedSegments.size(); i++) {
//        Segment closedSegment = preCompactedSegments.get(i);
//
//        try {
//          closeAndDeleteResults.get(i).get();
//        } catch (InterruptedException e) {
//          System.err.printf("Segment [%s] interrupted while being closed\n",
//              closedSegment.getSegmentFileKey());
//        } catch (ExecutionException e) {
//          System.err.printf("Segment [%s] failed while being closed. %s\n",
//              closedSegment.getSegmentFileKey(), e.getCause().getMessage());
//        }
//      }
//    } catch (InterruptedException e) {
//      System.err.println("Compactor interrupted while closing and deleting compacted segments");
//    }
//  }

  private record CompactionCompletionResultsImpl(List<Segment> compactedSegments,
                                                 List<Segment> preCompactionSegments) implements
      CompactionCompletionResults {

  }

}
