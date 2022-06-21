package dev.sbutler.bitflask.storage.segment;

import dev.sbutler.bitflask.storage.configuration.logging.InjectStorageLogger;
import dev.sbutler.bitflask.storage.segment.SegmentDeleter.DeletionResults;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import org.slf4j.Logger;

class SegmentManagerImpl implements SegmentManager {

  private static final int DEFAULT_COMPACTION_THRESHOLD_INCREMENT = 3;

  @InjectStorageLogger
  static Logger logger;

  private final SegmentFactory segmentFactory;
  private final SegmentCompactorFactory segmentCompactorFactory;
  private final SegmentDeleterFactory segmentDeleterFactory;

  private final AtomicReference<ManagedSegments> managedSegmentsAtomicReference = new AtomicReference<>();

  private final AtomicBoolean compactionActive = new AtomicBoolean(false);
  private final AtomicInteger nextCompactionThreshold = new AtomicInteger(
      DEFAULT_COMPACTION_THRESHOLD_INCREMENT);

  @Inject
  SegmentManagerImpl(SegmentFactory segmentFactory, SegmentLoader segmentLoader,
      SegmentCompactorFactory segmentCompactorFactory, SegmentDeleterFactory segmentDeleterFactory)
      throws IOException {
    this.segmentFactory = segmentFactory;
    this.segmentCompactorFactory = segmentCompactorFactory;
    this.segmentDeleterFactory = segmentDeleterFactory;
    initialize(segmentLoader);
  }

  private void initialize(SegmentLoader segmentLoader) throws IOException {
    boolean segmentStoreDirCreated = segmentFactory.createSegmentStoreDir();
    List<Segment> loadedSegments = segmentStoreDirCreated ? new ArrayList<>()
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
    SegmentCompactor segmentCompactor = segmentCompactorFactory.create(
        managedSegmentsAtomicReference.get().frozenSegments);
    segmentCompactor.registerCompactionCompletedConsumer(this::handleCompactionCompleted);
    segmentCompactor.registerCompactionFailedConsumer(this::handleCompactionFailed);
    segmentCompactor.compactSegments();
  }

  private void handleCompactionCompleted(
      SegmentCompactor.CompactionCompletionResults compactionCompletionResults) {
    updateAfterCompaction(compactionCompletionResults.compactedSegments());
    queuePreCompactionSegmentsForDeletion(compactionCompletionResults.preCompactionSegments());
    compactionActive.set(false);
  }

  private void handleCompactionFailed(Throwable throwable) {
    logger.error("Compaction failed", throwable);
    compactionActive.set(false);
  }

  private synchronized void updateAfterCompaction(List<Segment> compactedSegments) {
    logger.info("Updating after compaction");
    ManagedSegments currentManagedSegment = managedSegmentsAtomicReference.get();
    Deque<Segment> newFrozenSegments = new ArrayDeque<>();

    currentManagedSegment.frozenSegments.stream().filter((segment) -> !segment.hasBeenCompacted())
        .forEach(newFrozenSegments::offerFirst);
    newFrozenSegments.addAll(compactedSegments);

    nextCompactionThreshold.set(
        newFrozenSegments.size() + DEFAULT_COMPACTION_THRESHOLD_INCREMENT);

    managedSegmentsAtomicReference.set(
        new ManagedSegments(currentManagedSegment.writableSegment, newFrozenSegments));
  }

  private void queuePreCompactionSegmentsForDeletion(List<Segment> preCompactionSegments) {
    logger.info("Queueing segments for deletion after compaction");
    SegmentDeleter segmentDeleter = segmentDeleterFactory.create(preCompactionSegments);
    segmentDeleter.registerDeletionResultsConsumer(this::handleDeletionResults);
    segmentDeleter.deleteSegments();
  }

  private void handleDeletionResults(DeletionResults deletionResults) {
    switch (deletionResults.getStatus()) {
      case SUCCESS -> logger.info(buildLogForDeletionSuccess(deletionResults));
      case FAILED_GENERAL -> logger.error(buildLogForDeletionGeneralFailure(deletionResults),
          deletionResults.getGeneralFailureReason());
      case FAILED_SEGMENTS -> logger.error(buildLogForSegmentsFailure(deletionResults));
    }
  }

  private String buildLogForDeletionSuccess(DeletionResults deletionResults) {
    StringBuilder log = new StringBuilder();
    log.append("Compacted segments successfully deleted [");
    log.append(System.lineSeparator());
    deletionResults.getSegmentsToBeDeleted().forEach(segment -> {
      log.append(segment.getSegmentFileKey());
      log.append(System.lineSeparator());
    });
    log.append("]");
    return log.toString();
  }

  private String buildLogForDeletionGeneralFailure(DeletionResults deletionResults) {
    StringBuilder log = new StringBuilder();
    log.append("Failure to delete compacted segments due to general failure [");
    log.append(System.lineSeparator());
    deletionResults.getSegmentsToBeDeleted().forEach(segment -> {
      log.append(segment.getSegmentFileKey());
      log.append(System.lineSeparator());
    });
    log.append("]");
    return log.toString();
  }

  private String buildLogForSegmentsFailure(DeletionResults deletionResults) {
    StringBuilder log = new StringBuilder();
    log.append("Failure to delete compacted segments due to specific failures [");
    log.append(System.lineSeparator());
    deletionResults.getSegmentsFailureReasonsMap().forEach((segment, throwable) -> {
      log.append(segment.getSegmentFileKey());
      log.append(" , ");
      log.append(throwable.getMessage());
      log.append(System.lineSeparator());
    });
    log.append("]");
    return log.toString();
  }

  record ManagedSegments(Segment writableSegment, List<Segment> frozenSegments) {

    ManagedSegments(Segment writableSegment, Collection<Segment> frozenSegments) {
      this(writableSegment, List.copyOf(frozenSegments));
    }
  }
}
