package dev.sbutler.bitflask.storage.segment;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.storage.configuration.StorageConfiguration;
import dev.sbutler.bitflask.storage.configuration.concurrency.StorageExecutorService;
import dev.sbutler.bitflask.storage.segment.SegmentCompactor.CompactionResults;
import dev.sbutler.bitflask.storage.segment.SegmentDeleter.DeletionResults;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class SegmentManagerService extends AbstractService {

  public record ManagedSegments(Segment writableSegment,
                                ImmutableList<Segment> frozenSegments) {

  }

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ListeningExecutorService executorService;
  private final SegmentFactory segmentFactory;
  private final SegmentCompactor.Factory segmentCompactorFactory;
  private final SegmentDeleter.Factory segmentDeleterFactory;
  private final SegmentLoader segmentLoader;

  private final AtomicReference<ManagedSegments> managedSegmentsAtomicReference = new AtomicReference<>();

  private final int compactionThreshold;
  private final AtomicInteger nextCompactionThreshold = new AtomicInteger();

  private final ReentrantLock newSegmentLock = new ReentrantLock();
  private final AtomicBoolean creatingNewActiveSegment = new AtomicBoolean(false);
  private final ReentrantLock managedSegmentsLock = new ReentrantLock();
  private final ReentrantLock compactionLock = new ReentrantLock();
  private final AtomicBoolean compactionActive = new AtomicBoolean(false);

  @Inject
  SegmentManagerService(
      @StorageExecutorService ListeningExecutorService executorService,
      SegmentFactory segmentFactory,
      SegmentCompactor.Factory segmentCompactorFactory,
      SegmentDeleter.Factory segmentDeleterFactory,
      SegmentLoader segmentLoader,
      StorageConfiguration storageConfiguration) {
    this.executorService = executorService;
    this.segmentFactory = segmentFactory;
    this.segmentCompactorFactory = segmentCompactorFactory;
    this.segmentDeleterFactory = segmentDeleterFactory;
    this.segmentLoader = segmentLoader;
    this.compactionThreshold = storageConfiguration.getStorageCompactionThreshold();
  }

  @Override
  protected void doStart() {
    try {
      ManagedSegments managedSegments = segmentLoader.loadExistingSegments();
      managedSegments.writableSegment()
          .registerSizeLimitExceededConsumer(this::segmentSizeLimitExceededConsumer);
      managedSegmentsAtomicReference.set(managedSegments);
      nextCompactionThreshold.set(compactionThreshold + managedSegments.frozenSegments().size());
    } catch (IOException e) {
      notifyFailed(e);
    }
    notifyStarted();
  }

  @Override
  protected void doStop() {
    ManagedSegments managedSegments = managedSegmentsAtomicReference.get();
    managedSegments.writableSegment().close();
    managedSegments.frozenSegments().forEach(Segment::close);
    notifyStopped();
  }

  public ManagedSegments getManagedSegments() {
    return managedSegmentsAtomicReference.get();
  }

  /**
   * Called by a Segment once its size limit has been exceeded initiating manger update logic.
   *
   * <p>Update entails creating a new active write segment and, if needed, performing compaction
   * and deletion.
   */
  private void segmentSizeLimitExceededConsumer(Segment segment) {
    // Attempt, without blocking calling segment, to reduce scheduling jobs that will instantly complete
    if (shouldSkipCreatingNewActiveSegment(segment)) {
      return;
    }

    try {
      Futures.submit(() -> {
        handleSegmentSizeLimitExceeded(segment);
        ImmutableList<Segment> segmentsForDeletion = handleInitiatingCompaction();
        queueSegmentsForDeletion(segmentsForDeletion);
      }, executorService);
    } catch (RejectedExecutionException e) {
      logger.atSevere().withCause(e).log("Failed to submit creation of new active segment");
      notifyFailed(e);
    }
  }

  /**
   * Determines if a new active write segment should be created, and initiations the process if so.
   */
  private void handleSegmentSizeLimitExceeded(Segment segment) {
    newSegmentLock.lock();
    try {
      if (shouldSkipCreatingNewActiveSegment(segment)) {
        return;
      }
      creatingNewActiveSegment.set(true);
    } finally {
      newSegmentLock.unlock();
    }

    try {
      createNewWritableSegmentAndUpdateManagedSegments();
    } catch (IOException e) {
      logger.atSevere().withCause(e).log("Failed to create new active segment.");
      notifyFailed(e);
    } finally {
      creatingNewActiveSegment.set(false);
    }
  }

  private boolean shouldSkipCreatingNewActiveSegment(Segment segment) {
    return creatingNewActiveSegment.get() || !segment.equals(
        managedSegmentsAtomicReference.get().writableSegment());
  }

  /**
   * Creates a new writable segment and updates the managed segments accordingly.
   *
   * @throws IOException if there is an issue creating a new segment
   */
  private void createNewWritableSegmentAndUpdateManagedSegments() throws IOException {
    managedSegmentsLock.lock();
    try {
      logger.atInfo().log("Creating new active segment");
      ManagedSegments currentManagedSegments = managedSegmentsAtomicReference.get();
      Segment newWritableSegment = segmentFactory.createSegment();
      ImmutableList<Segment> newFrozenSegments = new ImmutableList.Builder<Segment>()
          .add(currentManagedSegments.writableSegment())
          .addAll(currentManagedSegments.frozenSegments())
          .build();

      newWritableSegment.registerSizeLimitExceededConsumer(this::segmentSizeLimitExceededConsumer);
      managedSegmentsAtomicReference.set(
          new ManagedSegments(newWritableSegment, newFrozenSegments));
    } finally {
      managedSegmentsLock.unlock();
    }
  }

  /**
   * Determines if a compaction should be started, and initiates the process if so.
   */
  private ImmutableList<Segment> handleInitiatingCompaction() {
    compactionLock.lock();
    try {
      if (shouldSkipCompaction()) {
        return ImmutableList.of();
      }
      compactionActive.set(true);
    } finally {
      compactionLock.unlock();
    }

    try {
      return initiateCompaction();
    } finally {
      compactionActive.set(false);
    }
  }

  private boolean shouldSkipCompaction() {
    return compactionActive.get()
        || managedSegmentsAtomicReference.get().frozenSegments.size()
        < nextCompactionThreshold.get();
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
      ManagedSegments currentManagedSegment = managedSegmentsAtomicReference.get();
      Deque<Segment> newFrozenSegments = new ArrayDeque<>();

      currentManagedSegment.frozenSegments().stream()
          .filter((segment) -> !segment.hasBeenCompacted())
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

  private void queueSegmentsForDeletion(ImmutableList<Segment> segmentsForDeletion) {
    if (segmentsForDeletion.isEmpty()) {
      return;
    }
    logger.atInfo()
        .log("Queueing [%d] segments for deletion after compaction", segmentsForDeletion.size());
    SegmentDeleter segmentDeleter = segmentDeleterFactory.create(segmentsForDeletion);
    ListenableFuture<DeletionResults> deletionResults = segmentDeleter.deleteSegments();
    Futures.addCallback(deletionResults, new DeletionResultsFutureCallback(),
        executorService);
  }

  // TODO: improve error handling
  private class DeletionResultsFutureCallback implements FutureCallback<DeletionResults> {

    @Override
    public void onSuccess(DeletionResults results) {
      switch (results) {
        case DeletionResults.Success success -> handleDeletionSuccess(success);
        case DeletionResults.FailedGeneral failedGeneral ->
            handleDeletionFailedGeneral(failedGeneral);
        case DeletionResults.FailedSegments failedSegments ->
            handleDeletionFailedSegments(failedSegments);
      }
    }

    @Override
    public void onFailure(@Nonnull Throwable t) {
      logger.atSevere().withCause(t).log("Segment deletion threw an unexpected exception");
      notifyFailed(t);
    }

    private void handleDeletionSuccess(DeletionResults.Success results) {
      logger.atInfo().log("Compacted segments successfully deleted "
          + buildLogForSegmentsToBeDeleted(results.segmentsProvidedForDeletion()));
    }

    private void handleDeletionFailedGeneral(DeletionResults.FailedGeneral results) {
      logger.atSevere().withCause(results.failureReason())
          .log("Failure to delete compacted segments due to general failure"
              + buildLogForSegmentsToBeDeleted(results.segmentsProvidedForDeletion()));
    }

    private void handleDeletionFailedSegments(DeletionResults.FailedSegments results) {
      logger.atSevere().log(buildLogForSegmentsFailure(results.segmentsFailureReasonsMap()));
    }

    private String buildLogForSegmentsToBeDeleted(ImmutableList<Segment> segmentsToBeDeleted) {
      return "[" + Joiner.on(", ").join(segmentsToBeDeleted) + "]";
    }

    private String buildLogForSegmentsFailure(
        ImmutableMap<Segment, Throwable> segmentThrowableImmutableMap) {
      Joiner.MapJoiner joiner = Joiner.on("; ").withKeyValueSeparator(", ");
      return "Failure to delete compacted segments due to specific failures [" +
          joiner.join(segmentThrowableImmutableMap) + "]";
    }

  }
}
