package dev.sbutler.bitflask.storage.segment;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
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

class SegmentManagerImpl implements SegmentManager {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final int DEFAULT_COMPACTION_THRESHOLD_INCREMENT = 3;

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
    ImmutableList<Segment> loadedSegments = segmentStoreDirCreated ? ImmutableList.of()
        : segmentLoader.loadExistingSegments();

    Segment writableSegment;
    if (loadedSegments.isEmpty()) {
      writableSegment = segmentFactory.createSegment();
    } else if (loadedSegments.get(0).exceedsStorageThreshold()) {
      writableSegment = segmentFactory.createSegment();
    } else {
      writableSegment = loadedSegments.get(0);
      loadedSegments = loadedSegments.subList(1, loadedSegments.size());
    }

    this.managedSegmentsAtomicReference.set(new ManagedSegments(writableSegment, loadedSegments));
  }

  @Override
  public Optional<String> read(String key) throws IOException {
    Optional<Segment> optionalSegment = findLatestSegmentWithKey(key);
    if (optionalSegment.isEmpty()) {
      logger.atInfo().log("Could not find a segment containing key [%s]", key);
      return Optional.empty();
    }
    Segment segment = optionalSegment.get();
    logger.atInfo()
        .log("Reading value of [%s] from segment [%d]", key, segment.getSegmentFileKey());
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
    logger.atInfo().log("Writing [%s] : [%s] to segment [%d]", key, value,
        writableSegment.getSegmentFileKey());
    writableSegment.write(key, value);
    checkWritableSegmentAndCompaction();
  }

  @Override
  public void close() {
    ManagedSegments managedSegments = managedSegmentsAtomicReference.get();
    managedSegments.writableSegment.close();
    managedSegments.frozenSegments.forEach(Segment::close);
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
    logger.atInfo().log("Creating new active segment");
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
    logger.atInfo().log("Initiating Compaction");
    compactionActive.set(true);
    SegmentCompactor segmentCompactor = segmentCompactorFactory.create(
        managedSegmentsAtomicReference.get().frozenSegments);
    segmentCompactor.registerCompactionCompletedConsumer(this::handleCompactionCompleted);
    segmentCompactor.registerCompactionFailedConsumer(this::handleCompactionFailed);
    segmentCompactor.compactSegments();
  }

  private void handleCompactionCompleted(
      SegmentCompactor.CompactionCompletionResults compactionCompletionResults) {
    logger.atInfo().log("Compaction completed");
    updateAfterCompaction(compactionCompletionResults.compactedSegments());
    queueSegmentsForDeletion(compactionCompletionResults.preCompactionSegments());
    compactionActive.set(false);
  }

  private void handleCompactionFailed(Throwable throwable, List<Segment> failedCompactionSegments) {
    logger.atSevere().withCause(throwable).log("Compaction failed");
    queueSegmentsForDeletion(failedCompactionSegments);
    compactionActive.set(false);
  }

  private synchronized void updateAfterCompaction(List<Segment> compactedSegments) {
    logger.atInfo().log("Updating after compaction");
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

  private void queueSegmentsForDeletion(List<Segment> segmentsForDeletion) {
    logger.atInfo().log("Queueing segments for deletion after compaction");
    SegmentDeleter segmentDeleter = segmentDeleterFactory.create(segmentsForDeletion);
    segmentDeleter.registerDeletionResultsConsumer(this::handleDeletionResults);
    segmentDeleter.deleteSegments();
  }

  private void handleDeletionResults(DeletionResults deletionResults) {
    switch (deletionResults.getStatus()) {
      case SUCCESS -> logger.atInfo().log(buildLogForDeletionSuccess(deletionResults));
      case FAILED_GENERAL -> logger.atSevere().withCause(deletionResults.getGeneralFailureReason())
          .log(buildLogForDeletionGeneralFailure(deletionResults));
      case FAILED_SEGMENTS -> logger.atSevere().log(buildLogForSegmentsFailure(deletionResults));
    }
  }

  private String buildLogForDeletionSuccess(DeletionResults deletionResults) {
    return "Compacted segments successfully deleted "
        + buildLogForSegmentsToBeDeleted(deletionResults.getSegmentsToBeDeleted());
  }

  private String buildLogForDeletionGeneralFailure(DeletionResults deletionResults) {
    return "Failure to delete compacted segments due to general failure"
        + buildLogForSegmentsToBeDeleted(deletionResults.getSegmentsToBeDeleted());
  }

  private String buildLogForSegmentsToBeDeleted(List<Segment> segmentsToBeDeleted) {
    StringBuilder log = new StringBuilder();
    log.append("[");
    for (int i = 0; i < segmentsToBeDeleted.size(); i++) {
      log.append(segmentsToBeDeleted.get(i).getSegmentFileKey());
      if (i + 1 < segmentsToBeDeleted.size()) {
        log.append(",");
      }
    }
    log.append("]");
    return log.toString();
  }

  private String buildLogForSegmentsFailure(DeletionResults deletionResults) {
    StringBuilder log = new StringBuilder();
    log.append("Failure to delete compacted segments due to specific failures [");
    deletionResults.getSegmentsFailureReasonsMap().forEach((segment, throwable) -> {
      log.append("(");
      log.append(segment.getSegmentFileKey());
      log.append(", ");
      log.append(throwable.getMessage());
      log.append(")");
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
