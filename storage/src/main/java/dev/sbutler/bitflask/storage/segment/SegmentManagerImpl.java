package dev.sbutler.bitflask.storage.segment;

import dev.sbutler.bitflask.storage.configuration.concurrency.StorageExecutorService;
import dev.sbutler.bitflask.storage.configuration.logging.InjectStorageLogger;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import org.slf4j.Logger;

class SegmentManagerImpl implements SegmentManager {

  private static final int DEFAULT_COMPACTION_THRESHOLD_INCREMENT = 3;

  @InjectStorageLogger
  static Logger logger;

  private final ExecutorService executorService;
  private final SegmentFactory segmentFactory;

  private Deque<Segment> frozenSegmentsDeque;
  private Segment activeSegment;

  private Future<Deque<Segment>> compactionFuture = null;
  private final AtomicBoolean isCompactionFinalizing = new AtomicBoolean(false);
  private final AtomicInteger nextCompactionThreshold = new AtomicInteger(
      DEFAULT_COMPACTION_THRESHOLD_INCREMENT);

  @Inject
  SegmentManagerImpl(@StorageExecutorService ExecutorService executorService,
      SegmentFactory segmentFactory, SegmentLoader segmentLoader)
      throws IOException {
    this.executorService = executorService;
    this.segmentFactory = segmentFactory;
    initialize(segmentLoader);
  }

  private void initialize(SegmentLoader segmentLoader) throws IOException {
    boolean segmentStoreDirCreated = segmentFactory.createSegmentStoreDir();
    Deque<Segment> loadedSegments = segmentStoreDirCreated ? new ConcurrentLinkedDeque<>()
        : segmentLoader.loadExistingSegments();

    if (loadedSegments.isEmpty()) {
      activeSegment = segmentFactory.createSegment();
    } else if (loadedSegments.peekFirst().exceedsStorageThreshold()) {
      activeSegment = segmentFactory.createSegment();
    } else {
      activeSegment = loadedSegments.pollFirst();
    }

    this.frozenSegmentsDeque = loadedSegments;
  }

  @Override
  public Optional<String> read(String key) throws IOException {
    Optional<Segment> optionalSegment = findLatestSegmentWithKey(key);
    if (optionalSegment.isEmpty()) {
      logger.info("Could not find a segment containing key [{}]", key);
      return Optional.empty();
    }
    Segment segment = optionalSegment.get();
    logger.info("Reading value of [{}] from segment [{}]", key, segment.getSegmentFileKey());
    return segment.read(key);
  }

  private Optional<Segment> findLatestSegmentWithKey(String key) {
    if (activeSegment.containsKey(key)) {
      return Optional.of(activeSegment);
    }
    for (Segment segment : frozenSegmentsDeque) {
      if (segment.containsKey(key)) {
        return Optional.of(segment);
      }
    }
    return Optional.empty();
  }

  @Override
  public void write(String key, String value) throws IOException {
    logger.info("Writing [{}] : [{}] to segment [{}]", key, value,
        activeSegment.getSegmentFileKey());
    activeSegment.write(key, value);
    checkActiveSegmentAndCompaction();
  }

  private synchronized void checkActiveSegmentAndCompaction() throws IOException {
    checkActiveSegment();
    checkCompaction();
  }

  private void checkActiveSegment() throws IOException {
    if (activeSegment.exceedsStorageThreshold()) {
      createNewActiveSegmentAndUpdate();
    }
  }

  private void createNewActiveSegmentAndUpdate() throws IOException {
    frozenSegmentsDeque.offerFirst(activeSegment);
    activeSegment = segmentFactory.createSegment();
  }

  private void checkCompaction() {
    if (isCompactionActive()) {
      if (shouldFinalizeCompaction()) {
        finalizeCompaction();
      }
    } else if (shouldPerformCompaction()) {
      initiateCompaction();
    }
  }

  private synchronized boolean isCompactionActive() {
    return compactionFuture != null || isCompactionFinalizing.get();
  }

  private synchronized boolean shouldFinalizeCompaction() {
    return !isCompactionFinalizing.get();
  }

  private synchronized boolean shouldPerformCompaction() {
    return frozenSegmentsDeque.size() >= nextCompactionThreshold.get();
  }

  private void initiateCompaction() {
    SegmentCompactor compactor = new SegmentCompactor(segmentFactory, frozenSegmentsDeque);
    compactionFuture = executorService.submit(compactor);
  }

  private void finalizeCompaction() {
    isCompactionFinalizing.set(true);
    executorService.submit(() -> performCompactionFinalization(compactionFuture));
    compactionFuture = null;
  }

  private void performCompactionFinalization(Future<Deque<Segment>> compactionFuture) {
    try {
      Deque<Segment> compactedSegments = compactionFuture.get();
      Deque<Segment> newFrozenDeque = new ConcurrentLinkedDeque<>();
      List<Segment> preCompactionSegments = new ArrayList<>();

      for (Segment segment : frozenSegmentsDeque) {
        if (segment.hasBeenCompacted()) {
          preCompactionSegments.add(segment);
        } else {
          newFrozenDeque.offerLast(segment);
        }
      }
      newFrozenDeque.addAll(compactedSegments);
      nextCompactionThreshold.set(newFrozenDeque.size() + DEFAULT_COMPACTION_THRESHOLD_INCREMENT);
      frozenSegmentsDeque = newFrozenDeque;

      closeAndDeleteSegments(preCompactionSegments);
    } catch (InterruptedException | ExecutionException e) {
      logger.error("Compaction could not be completed", e);
    } finally {
      isCompactionFinalizing.set(false);
    }
  }

  private void closeAndDeleteSegments(List<Segment> segments) {
    for (Segment segment : segments) {
      try {
        segment.closeAndDelete();
      } catch (IOException e) {
        System.err.println("Failure to close segment: " + e.getMessage());
      }
    }
  }

}
