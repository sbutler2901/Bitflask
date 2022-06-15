package dev.sbutler.bitflask.storage.segment;

import dev.sbutler.bitflask.storage.configuration.logging.InjectStorageLogger;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.inject.Provider;
import org.slf4j.Logger;

class SegmentManagerImpl implements SegmentManager {

  private static final int DEFAULT_COMPACTION_THRESHOLD_INCREMENT = 3;

  @InjectStorageLogger
  static Logger logger;

  private final SegmentFactory segmentFactory;
  private final Provider<SegmentCompactor> segmentCompactorProvider;

  private List<Segment> frozenSegments;
  private Segment activeSegment;

  private final AtomicBoolean compactionActive = new AtomicBoolean(false);
  private final AtomicInteger nextCompactionThreshold = new AtomicInteger(
      DEFAULT_COMPACTION_THRESHOLD_INCREMENT);

  @Inject
  SegmentManagerImpl(SegmentFactory segmentFactory, SegmentLoader segmentLoader,
      Provider<SegmentCompactor> segmentCompactorProvider)
      throws IOException {
    this.segmentFactory = segmentFactory;
    this.segmentCompactorProvider = segmentCompactorProvider;
    initialize(segmentLoader);
  }

  private void initialize(SegmentLoader segmentLoader) throws IOException {
    boolean segmentStoreDirCreated = segmentFactory.createSegmentStoreDir();
    List<Segment> loadedSegments = segmentStoreDirCreated ? new CopyOnWriteArrayList<>()
        : segmentLoader.loadExistingSegments();

    if (loadedSegments.isEmpty()) {
      activeSegment = segmentFactory.createSegment();
    } else if (loadedSegments.get(0).exceedsStorageThreshold()) {
      activeSegment = segmentFactory.createSegment();
    } else {
      activeSegment = loadedSegments.remove(0);
    }

    this.frozenSegments = loadedSegments;
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
    for (Segment segment : frozenSegments) {
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
    if (shouldCreateAndUpdateActiveSegment()) {
      createNewActiveSegmentAndUpdate();
    }
    if (shouldInitiateCompaction()) {
      initiateCompaction();
    }
  }

  private boolean shouldCreateAndUpdateActiveSegment() {
    return activeSegment.exceedsStorageThreshold();
  }

  private void createNewActiveSegmentAndUpdate() throws IOException {
    logger.info("Creating new active segment");
    frozenSegments.add(0, activeSegment);
    activeSegment = segmentFactory.createSegment();
  }

  private boolean shouldInitiateCompaction() {
    return !compactionActive.get() && frozenSegments.size() >= nextCompactionThreshold.get();
  }

  private void initiateCompaction() {
    logger.info("Initiating Compaction");
    compactionActive.set(true);
    SegmentCompactor segmentCompactor = segmentCompactorProvider.get();
    segmentCompactor.setPreCompactedSegments(frozenSegments);
    segmentCompactor.registerCompactionResultsConsumer(this::updateAfterCompaction);
    segmentCompactor.registerCompactionCompletedRunnable(this::compactionCompleted);
    segmentCompactor.registerCompactionFailedConsumer(this::compactionFailed);
    segmentCompactor.compactSegments();
  }

  private synchronized void updateAfterCompaction(List<Segment> compactedSegments) {
    logger.info("Updating after compaction");
    Deque<Segment> newFrozenSegments = new ArrayDeque<>();
    frozenSegments.stream().filter((segment) -> !segment.hasBeenCompacted())
        .forEach(newFrozenSegments::offerFirst);
    newFrozenSegments.addAll(compactedSegments);
    nextCompactionThreshold.set(
        newFrozenSegments.size() + DEFAULT_COMPACTION_THRESHOLD_INCREMENT);
    frozenSegments = new CopyOnWriteArrayList<>(newFrozenSegments);
  }

  private void compactionCompleted() {
    logger.info("Segment compaction completed");
    compactionActive.set(false);
  }

  private void compactionFailed(Throwable throwable) {
    logger.error("Compaction failed", throwable);
    compactionActive.set(false);
  }

}
