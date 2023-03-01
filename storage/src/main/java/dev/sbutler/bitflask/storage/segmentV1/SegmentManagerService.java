package dev.sbutler.bitflask.storage.segmentV1;

import static java.util.function.Predicate.not;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.segmentV1.SegmentCompactor.CompactionResults;
import dev.sbutler.bitflask.storage.segmentV1.SegmentDeleter.DeletionResults;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Manages and maintains the Segments used by the storage engine.
 *
 * <p>Handles providing the active {@link WritableSegment} as well as frozen
 * {@link ReadableSegment}s available for reading.
 *
 * <p>Updates the active {@link WritableSegment} when its size limit threshold has been reached
 * creating a new one and freezes the current one.
 *
 * <p>Periodically compacts the frozen segments, removing outdated key:value pairs, and deleting
 * the old segments.
 */
@Singleton
public final class SegmentManagerService extends AbstractService {

  record ManagedSegments(Segment writableSegment,
                         ImmutableList<Segment> frozenSegments) {

  }

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ListeningExecutorService listeningExecutorService;
  private final SegmentFactory segmentFactory;
  private final SegmentCompactor.Factory segmentCompactorFactory;
  private final SegmentDeleter.Factory segmentDeleterFactory;
  private final SegmentLoader segmentLoader;

  private final AtomicReference<ManagedSegments> managedSegmentsAtomicReference = new AtomicReference<>();

  private final int compactionThreshold;
  private final AtomicInteger nextCompactionThreshold = new AtomicInteger();

  private final ReentrantLock managedSegmentsLock = new ReentrantLock();
  private final ReentrantLock compactionLock = new ReentrantLock();

  @Inject
  SegmentManagerService(
      ListeningExecutorService listeningExecutorService,
      SegmentFactory segmentFactory,
      SegmentCompactor.Factory segmentCompactorFactory,
      SegmentDeleter.Factory segmentDeleterFactory,
      SegmentLoader segmentLoader,
      StorageConfigurations storageConfigurations) {
    this.listeningExecutorService = listeningExecutorService;
    this.segmentFactory = segmentFactory;
    this.segmentCompactorFactory = segmentCompactorFactory;
    this.segmentDeleterFactory = segmentDeleterFactory;
    this.segmentLoader = segmentLoader;
    this.compactionThreshold = storageConfigurations.getStorageCompactionThreshold();
  }

  @Override
  protected void doStart() {
    Futures.submit(() -> {
      try {
        ManagedSegments managedSegments = segmentLoader.loadExistingSegments();
        managedSegments.writableSegment()
            .registerSizeLimitExceededConsumer(this::segmentSizeLimitExceededConsumer);
        managedSegmentsAtomicReference.set(managedSegments);
        nextCompactionThreshold.set(compactionThreshold + managedSegments.frozenSegments().size());
        notifyStarted();
      } catch (Exception e) {
        notifyFailed(e);
      }
    }, listeningExecutorService);
  }

  @Override
  protected void doStop() {
    Futures.submit(() -> {
      try {
        ManagedSegments managedSegments = managedSegmentsAtomicReference.get();
        managedSegments.writableSegment().close();
        managedSegments.frozenSegments().forEach(Segment::close);
        notifyStopped();
      } catch (Exception e) {
        notifyFailed(e);
      }
    }, listeningExecutorService);
  }

  ManagedSegments getManagedSegments() {
    return managedSegmentsAtomicReference.get();
  }

  public WritableSegment getWritableSegment() {
    return managedSegmentsAtomicReference.get().writableSegment();
  }

  public ImmutableList<ReadableSegment> getReadableSegments() {
    ImmutableList.Builder<ReadableSegment> readableSegments = new ImmutableList.Builder<>();
    ManagedSegments managedSegments = managedSegmentsAtomicReference.get();
    readableSegments.add(managedSegments.writableSegment());
    readableSegments.addAll(managedSegments.frozenSegments());
    return readableSegments.build();
  }

  /**
   * Called by a Segment once its size limit has been exceeded initiating manager update logic.
   *
   * <p>Update entails creating a new active write segment and, if needed, performing compaction
   * and deletion.
   */
  private void segmentSizeLimitExceededConsumer(Segment segment) {
    try {
      Futures.submit(createSizeLimitExceededTask(segment), listeningExecutorService);
    } catch (RejectedExecutionException e) {
      logger.atSevere().withCause(e)
          .log("Failed to submit cleanup task after segment size limit exceeded");
      notifyFailed(e);
    }
  }

  /**
   * The task to be executed when a Segment indicates its size limit has been reached.
   *
   * <p>This task may create a new writable segment, perform compaction, and perform
   * post-compaction deletion. Compaction may be activated when a new segment is created and
   * deletion will be activated if compaction is activated.
   */
  private Runnable createSizeLimitExceededTask(Segment segment) {
    return () -> {
      try {
        boolean segmentCreated = handleCreatingNewSegment(segment);
        if (!segmentCreated) {
          return;
        }
        ImmutableList<Segment> segmentsForDeletion = handleInitiatingCompaction();
        if (segmentsForDeletion.isEmpty()) {
          return;
        }
        initiateDeletion(segmentsForDeletion);
      } catch (Exception e) {
        logger.atSevere().withCause(e)
            .log("Failed to execute size limit exceeded task");
        notifyFailed(e);
      }
      logger.atInfo().log("Successfully executed size limit exceeded task");
    };
  }

  /**
   * Determines if a new active write segment should be created, and initiations the process if so.
   *
   * @return whether a new segment was created
   */
  private boolean handleCreatingNewSegment(Segment segment) throws IOException {
    if (managedSegmentsLock.tryLock()) {
      try {
        if (segment.equals(managedSegmentsAtomicReference.get().writableSegment())) {
          createNewWritableSegmentAndUpdateManagedSegments();
          return true;
        }
      } finally {
        managedSegmentsLock.unlock();
      }
    }
    return false;
  }

  /**
   * Creates a new writable segment and updates the managed segments accordingly.
   *
   * @throws IOException if there is an issue creating a new segment
   */
  private void createNewWritableSegmentAndUpdateManagedSegments() throws IOException {
    logger.atInfo().log("Creating new active segment");

    Segment newWritableSegment = segmentFactory.createSegment();
    newWritableSegment.registerSizeLimitExceededConsumer(this::segmentSizeLimitExceededConsumer);

    ManagedSegments currentManagedSegments = managedSegmentsAtomicReference.get();
    ImmutableList<Segment> newFrozenSegments = new ImmutableList.Builder<Segment>()
        .add(currentManagedSegments.writableSegment())
        .addAll(currentManagedSegments.frozenSegments())
        .build();

    managedSegmentsAtomicReference.set(
        new ManagedSegments(newWritableSegment, newFrozenSegments));
  }

  /**
   * Determines if a compaction should be started, and initiates the process if so.
   */
  private ImmutableList<Segment> handleInitiatingCompaction() {
    if (compactionLock.tryLock()) {
      try {
        if (managedSegmentsAtomicReference.get().frozenSegments().size()
            >= nextCompactionThreshold.get()) {
          return initiateCompaction();
        }
      } finally {
        compactionLock.unlock();
      }
    }
    return ImmutableList.of();
  }

  /**
   * Initiates compaction, processes the results, and returns any segments available for deletion
   */
  private ImmutableList<Segment> initiateCompaction() {
    logger.atInfo().log("Initiating Compaction");
    SegmentCompactor segmentCompactor = segmentCompactorFactory.create(
        managedSegmentsAtomicReference.get().frozenSegments());
    CompactionResults compactionResults = segmentCompactor.compactSegments();
    return handleCompactionResults(compactionResults);
  }

  /**
   * Processes the compaction results, updating the managed segments if successful, and returns any
   * segments available for deletion.
   */
  private ImmutableList<Segment> handleCompactionResults(CompactionResults compactionResults) {
    return switch (compactionResults) {
      case CompactionResults.Success success -> {
        logger.atInfo().log("Compaction completed");
        updateAfterCompaction(success.compactedSegments());
        yield success.segmentsProvidedForCompaction();
      }
      case CompactionResults.Failed failed -> {
        logger.atSevere().withCause(failed.failureReason()).log("Compaction failed");
        yield failed.failedCompactionSegments();
      }
    };
  }

  /**
   * Updates the managed segments with the results of compaction
   */
  private void updateAfterCompaction(ImmutableList<Segment> compactedSegments) {
    managedSegmentsLock.lock();
    try {
      logger.atInfo().log("Updating after compaction");

      Deque<Segment> newFrozenSegments = new ArrayDeque<>();
      ManagedSegments currentManagedSegment = managedSegmentsAtomicReference.get();

      currentManagedSegment.frozenSegments().stream()
          .filter(not(Segment::hasBeenCompacted))
          .forEach(newFrozenSegments::offerFirst);
      newFrozenSegments.addAll(compactedSegments);

      nextCompactionThreshold.set(compactionThreshold + newFrozenSegments.size());

      managedSegmentsAtomicReference.set(
          new ManagedSegments(currentManagedSegment.writableSegment(),
              ImmutableList.copyOf(newFrozenSegments)));
    } finally {
      managedSegmentsLock.unlock();
    }
  }

  /**
   * Initiates deletion, processes and logs the results.
   */
  private void initiateDeletion(ImmutableList<Segment> segmentsForDeletion) {
    logger.atInfo()
        .log("Queueing [%d] segments for deletion after compaction", segmentsForDeletion.size());
    SegmentDeleter segmentDeleter = segmentDeleterFactory.create(segmentsForDeletion);
    DeletionResults deletionResults = segmentDeleter.deleteSegments();
    handleDeletionResults(deletionResults);
  }

  /**
   * Processes {@link DeletionResults} and logs the results.
   */
  private void handleDeletionResults(DeletionResults deletionResults) {
    switch (deletionResults) {
      case DeletionResults.Success success ->
          logger.atInfo().log("Compacted segments successfully deleted "
              + buildLogForSegments(success.segmentsProvidedForDeletion()));
      case DeletionResults.FailedGeneral failedGeneral ->
          logger.atSevere().withCause(failedGeneral.failureReason())
              .log("Failure to delete compacted segments due to general failure"
                  + buildLogForSegments(failedGeneral.segmentsProvidedForDeletion()));
      case DeletionResults.FailedSegments failedSegments ->
          logger.atSevere().log("Failure to delete compacted segments due to specific failures"
              + buildLogForSegmentsWithFailures(failedSegments.segmentsFailureReasonsMap()));
    }
  }

  private String buildLogForSegments(ImmutableList<Segment> segments) {
    return "[" + Joiner.on(", ").join(segments) + "]";
  }

  private String buildLogForSegmentsWithFailures(
      ImmutableMap<Segment, Throwable> segmentThrowableMap) {
    return "["
        + Joiner.on("; ")
        .withKeyValueSeparator(", ")
        .join(segmentThrowableMap)
        + "]";
  }
}