package dev.sbutler.bitflask.storage.segment;

import dev.sbutler.bitflask.storage.configuration.logging.InjectStorageLogger;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import javax.inject.Provider;
import org.slf4j.Logger;

class SegmentManagerImpl implements SegmentManager {

  private static final int DEFAULT_COMPACTION_THRESHOLD_INCREMENT = 3;

  @InjectStorageLogger
  static Logger logger;

  private final SegmentFactory segmentFactory;
  private final Provider<SegmentCompactor> segmentCompactorProvider;

  private final AtomicReference<ManagedSegments> managedSegmentsAtomicReference = new AtomicReference<>();

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

    Segment writableSegment;
    if (loadedSegments.isEmpty()) {
      writableSegment = segmentFactory.createSegment();
    } else if (loadedSegments.get(0).exceedsStorageThreshold()) {
      writableSegment = segmentFactory.createSegment();
    } else {
      writableSegment = loadedSegments.remove(0);
    }

    this.managedSegmentsAtomicReference.set(new ManagedSegments(writableSegment, loadedSegments));
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
    ManagedSegments managedSegments = managedSegmentsAtomicReference.get();
    if (managedSegments.writableSegment.containsKey(key)) {
      return Optional.of(managedSegments.writableSegment);
    }
    for (Segment segment : managedSegments.frozenSegments) {
      if (segment.containsKey(key)) {
        return Optional.of(segment);
      }
    }
    return Optional.empty();
  }

  @Override
  public void write(String key, String value) throws IOException {
    Segment writableSegment = managedSegmentsAtomicReference.get().writableSegment;
    logger.info("Writing [{}] : [{}] to segment [{}]", key, value,
        writableSegment.getSegmentFileKey());
    writableSegment.write(key, value);
    checkWritableSegmentAndCompaction();
  }

  private synchronized void checkWritableSegmentAndCompaction() throws IOException {
    if (shouldCreateAndUpdateWritableSegment()) {
      createNewWritableSegmentAndUpdateManagedSegments();
    }
    if (shouldInitiateCompaction()) {
      initiateCompaction();
    }
  }

  private boolean shouldCreateAndUpdateWritableSegment() {
    return managedSegmentsAtomicReference.get().writableSegment.exceedsStorageThreshold();
  }

  private synchronized void createNewWritableSegmentAndUpdateManagedSegments() throws IOException {
    logger.info("Creating new active segment");
    ManagedSegments currentManagedSegments = managedSegmentsAtomicReference.get();
    Segment newWritableSegment = segmentFactory.createSegment();
    List<Segment> newFrozenSegments = new ArrayList<>();

    newFrozenSegments.add(currentManagedSegments.writableSegment);
    newFrozenSegments.addAll(currentManagedSegments.frozenSegments);

    managedSegmentsAtomicReference.set(new ManagedSegments(newWritableSegment, newFrozenSegments));
  }

  private boolean shouldInitiateCompaction() {
    return !compactionActive.get() && managedSegmentsAtomicReference.get().frozenSegments.size()
        >= nextCompactionThreshold.get();
  }

  private void initiateCompaction() {
    logger.info("Initiating Compaction");
    compactionActive.set(true);
    SegmentCompactor segmentCompactor = segmentCompactorProvider.get();
    segmentCompactor.setPreCompactedSegments(managedSegmentsAtomicReference.get().frozenSegments);
    segmentCompactor.registerCompactionResultsConsumer(this::updateAfterCompaction);
    segmentCompactor.registerCompactionCompletedRunnable(this::compactionCompleted);
    segmentCompactor.registerCompactionFailedConsumer(this::compactionFailed);
    segmentCompactor.compactSegments();
  }

  private synchronized void updateAfterCompaction(List<Segment> compactedSegments) {
    logger.info("Updating after compaction");
    ManagedSegments currentManagedSegment = managedSegmentsAtomicReference.get();
    Deque<Segment> newFrozenSegments = new ArrayDeque<>();

    currentManagedSegment.frozenSegments.stream()
        .filter((segment) -> !segment.hasBeenCompacted())
        .forEach(newFrozenSegments::offerFirst);
    newFrozenSegments.addAll(compactedSegments);

    nextCompactionThreshold.set(
        newFrozenSegments.size() + DEFAULT_COMPACTION_THRESHOLD_INCREMENT);

    managedSegmentsAtomicReference.set(
        new ManagedSegments(currentManagedSegment.writableSegment, newFrozenSegments));
  }

  private void compactionCompleted() {
    logger.info("Segment compaction completed");
    compactionActive.set(false);
  }

  private void compactionFailed(Throwable throwable) {
    logger.error("Compaction failed", throwable);
    compactionActive.set(false);
  }


  record ManagedSegments(Segment writableSegment, List<Segment> frozenSegments) {

    ManagedSegments(Segment writableSegment, Collection<Segment> frozenSegments) {
      this(writableSegment, List.copyOf(frozenSegments));
    }
  }
}
