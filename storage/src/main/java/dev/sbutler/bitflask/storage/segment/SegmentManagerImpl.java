package dev.sbutler.bitflask.storage.segment;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.storage.segment.SegmentDeleter.DeletionResults;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
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
  SegmentManagerImpl(SegmentFactory segmentFactory, ManagedSegments managedSegments,
      SegmentCompactorFactory segmentCompactorFactory,
      SegmentDeleterFactory segmentDeleterFactory) {
    this.segmentFactory = segmentFactory;
    this.segmentCompactorFactory = segmentCompactorFactory;
    this.segmentDeleterFactory = segmentDeleterFactory;
    this.managedSegmentsAtomicReference.set(managedSegments);
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
    if (managedSegments.getWritableSegment().containsKey(key)) {
      return Optional.of(managedSegments.getWritableSegment());
    }
    for (Segment segment : managedSegments.getFrozenSegments()) {
      if (segment.containsKey(key)) {
        return Optional.of(segment);
      }
    }
    return Optional.empty();
  }

  @Override
  public void write(String key, String value) throws IOException {
    Segment writableSegment = managedSegmentsAtomicReference.get().getWritableSegment();
    logger.atInfo().log("Writing [%s] : [%s] to segment [%d]", key, value,
        writableSegment.getSegmentFileKey());
    writableSegment.write(key, value);
    checkWritableSegmentAndCompaction();
  }

  @Override
  public void close() {
    ManagedSegments managedSegments = managedSegmentsAtomicReference.get();
    managedSegments.getWritableSegment().close();
    managedSegments.getFrozenSegments().forEach(Segment::close);
  }

  /**
   * Determine if a new writable segment and / or compaction should be performed.
   *
   * @throws IOException if an error occurs creating a new writable segment.
   */
  private synchronized void checkWritableSegmentAndCompaction() throws IOException {
    if (shouldCreateAndUpdateWritableSegment()) {
      createNewWritableSegmentAndUpdateManagedSegments();
    }
    if (shouldInitiateCompaction()) {
      initiateCompaction();
    }
  }

  private boolean shouldCreateAndUpdateWritableSegment() {
    return managedSegmentsAtomicReference.get().getWritableSegment().exceedsStorageThreshold();
  }

  private synchronized void createNewWritableSegmentAndUpdateManagedSegments() throws IOException {
    logger.atInfo().log("Creating new active segment");
    ManagedSegments currentManagedSegments = managedSegmentsAtomicReference.get();
    Segment newWritableSegment = segmentFactory.createSegment();
    ImmutableList<Segment> newFrozenSegments = new ImmutableList.Builder<Segment>()
        .add(currentManagedSegments.getWritableSegment())
        .addAll(currentManagedSegments.getFrozenSegments())
        .build();

    managedSegmentsAtomicReference.set(
        new ManagedSegmentsImpl(newWritableSegment, newFrozenSegments));
  }

  private boolean shouldInitiateCompaction() {
    return !compactionActive.get()
        && managedSegmentsAtomicReference.get().getFrozenSegments().size()
        >= nextCompactionThreshold.get();
  }

  private void initiateCompaction() {
    logger.atInfo().log("Initiating Compaction");
    compactionActive.set(true);
    SegmentCompactor segmentCompactor = segmentCompactorFactory.create(
        managedSegmentsAtomicReference.get().getFrozenSegments());
    segmentCompactor.registerCompactionResultsConsumer(this::handleCompactionResults);
    segmentCompactor.compactSegments();
  }

  /**
   * Called by a compactor when compaction has completed successfully.
   *
   * @param compactionResults the results of compaction
   */
  private void handleCompactionResults(
      SegmentCompactor.CompactionResults compactionResults) {
    switch (compactionResults.getStatus()) {
      case SUCCESS -> handleCompactionSuccess(
          compactionResults.getCompactedSegments(),
          compactionResults.getSegmentsProvidedForCompaction()
      );
      case FAILED -> handleCompactionFailed(
          compactionResults.getFailureReason(),
          compactionResults.getFailedCompactedSegments()
      );
    }
  }

  private void handleCompactionSuccess(ImmutableList<Segment> compactedSegments,
      ImmutableList<Segment> segmentsProvidedForCompaction) {
    logger.atInfo().log("Compaction completed");
    updateAfterCompaction(compactedSegments);
    queueSegmentsForDeletion(segmentsProvidedForCompaction);
    compactionActive.set(false);
  }

  private void handleCompactionFailed(Throwable throwable,
      ImmutableList<Segment> failedCompactionSegments) {
    logger.atSevere().withCause(throwable).log("Compaction failed");
    queueSegmentsForDeletion(failedCompactionSegments);
    compactionActive.set(false);
  }

  private synchronized void updateAfterCompaction(ImmutableList<Segment> compactedSegments) {
    logger.atInfo().log("Updating after compaction");
    ManagedSegments currentManagedSegment = managedSegmentsAtomicReference.get();
    Deque<Segment> newFrozenSegments = new ArrayDeque<>();

    currentManagedSegment.getFrozenSegments().stream()
        .filter((segment) -> !segment.hasBeenCompacted())
        .forEach(newFrozenSegments::offerFirst);
    newFrozenSegments.addAll(compactedSegments);

    nextCompactionThreshold.set(
        newFrozenSegments.size() + DEFAULT_COMPACTION_THRESHOLD_INCREMENT);

    managedSegmentsAtomicReference.set(
        new ManagedSegmentsImpl(currentManagedSegment.getWritableSegment(),
            ImmutableList.copyOf(newFrozenSegments)));
  }

  private void queueSegmentsForDeletion(ImmutableList<Segment> segmentsForDeletion) {
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
        + buildLogForSegmentsToBeDeleted(deletionResults.getSegmentsProvidedForDeletion());
  }

  private String buildLogForDeletionGeneralFailure(DeletionResults deletionResults) {
    return "Failure to delete compacted segments due to general failure"
        + buildLogForSegmentsToBeDeleted(deletionResults.getSegmentsProvidedForDeletion());
  }

  private String buildLogForSegmentsToBeDeleted(ImmutableList<Segment> segmentsToBeDeleted) {
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

  record ManagedSegmentsImpl(Segment writableSegment,
                             ImmutableList<Segment> frozenSegments) implements ManagedSegments {

    @Override
    public Segment getWritableSegment() {
      return writableSegment;
    }

    @Override
    public ImmutableList<Segment> getFrozenSegments() {
      return frozenSegments;
    }
  }
}
