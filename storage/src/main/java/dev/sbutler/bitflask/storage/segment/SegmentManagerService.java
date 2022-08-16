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
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class SegmentManagerService extends AbstractService {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ListeningExecutorService executorService;
  private final SegmentFactory segmentFactory;
  private final SegmentCompactorFactory segmentCompactorFactory;
  private final SegmentDeleterFactory segmentDeleterFactory;
  private final SegmentLoader segmentLoader;

  private final AtomicReference<ManagedSegments> managedSegmentsAtomicReference = new AtomicReference<>();

  private final int compactionThreshold;
  private final AtomicInteger nextCompactionThreshold = new AtomicInteger();

  private final AtomicBoolean creatingNewActiveSegment = new AtomicBoolean(false);
  private final AtomicBoolean compactionActive = new AtomicBoolean(false);

  @Inject
  SegmentManagerService(
      @StorageExecutorService ListeningExecutorService executorService,
      SegmentFactory segmentFactory,
      SegmentCompactorFactory segmentCompactorFactory,
      SegmentDeleterFactory segmentDeleterFactory,
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

  public record ManagedSegments(Segment writableSegment,
                                ImmutableList<Segment> frozenSegments) {

  }

  private void segmentSizeLimitExceededConsumer(Segment segment) {
    // Attempt, without blocking calling segment, to reduce scheduling jobs that will instantly complete
    if (shouldSkipCreatingNewActiveSegment(segment)) {
      return;
    }

    try {
      Futures.submit(() -> handleSegmentSizeLimitExceeded(segment), executorService);
    } catch (RejectedExecutionException e) {
      logger.atSevere().withCause(e).log("Failed to submit creation of new active segment");
      notifyFailed(e);
    }
  }

  private synchronized void handleSegmentSizeLimitExceeded(Segment segment) {
    if (shouldSkipCreatingNewActiveSegment(segment)) {
      return;
    }

    creatingNewActiveSegment.set(true);
    try {
      createNewWritableSegmentAndUpdateManagedSegments();
    } catch (IOException e) {
      logger.atSevere().withCause(e).log("Failed to create new active segment.");
      notifyFailed(e);
    } finally {
      creatingNewActiveSegment.set(false);
    }

    // TODO: simplify compaction activation / handling
    if (shouldInitiateCompaction()) {
      initiateCompaction();
    }
  }

  private boolean shouldSkipCreatingNewActiveSegment(Segment segment) {
    return creatingNewActiveSegment.get() || !segment.equals(
        managedSegmentsAtomicReference.get().writableSegment());
  }

  private synchronized void createNewWritableSegmentAndUpdateManagedSegments() throws IOException {
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
  }

  private boolean shouldInitiateCompaction() {
    return !compactionActive.get()
        && managedSegmentsAtomicReference.get().frozenSegments().size()
        >= nextCompactionThreshold.get();
  }

  private synchronized void initiateCompaction() {
    logger.atInfo().log("Initiating Compaction");
    compactionActive.set(true);
    SegmentCompactor segmentCompactor = segmentCompactorFactory.create(
        managedSegmentsAtomicReference.get().frozenSegments());
    ListenableFuture<CompactionResults> compactionResults = segmentCompactor.compactSegments();
    Futures.addCallback(compactionResults, new CompactionResultsFutureCallback(), executorService);
  }

  private synchronized void updateAfterCompaction(ImmutableList<Segment> compactedSegments) {
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

  private class CompactionResultsFutureCallback implements FutureCallback<CompactionResults> {

    @Override
    public void onSuccess(CompactionResults result) {
      switch (result) {
        case CompactionResults.Success success -> handleCompactionSuccess(success);
        case CompactionResults.Failed failed -> handleCompactionFailed(failed);
      }
      compactionActive.set(false);
    }

    @Override
    public void onFailure(@Nonnull Throwable t) {
      logger.atSevere().withCause(t).log("Segment compaction threw an unexpected exception");
      compactionActive.set(false);
      notifyFailed(t);
    }

    private void handleCompactionSuccess(CompactionResults.Success success) {
      logger.atInfo().log("Compaction completed");
      updateAfterCompaction(success.compactedSegments());
      queueSegmentsForDeletion(success.segmentsProvidedForCompaction());
    }

    private void handleCompactionFailed(CompactionResults.Failed failed) {
      logger.atSevere().withCause(failed.failureReason()).log("Compaction failed");
      queueSegmentsForDeletion(failed.failedCompactionSegments());
    }
  }

  // TODO: improve error handling
  private record DeletionResultsFutureCallback() implements FutureCallback<DeletionResults> {

    @Override
    public void onSuccess(DeletionResults results) {
      switch (results) {
        case DeletionResults.Success success -> handleDeletionSuccess(success);
        case DeletionResults.FailedGeneral failedGeneral -> handleDeletionFailedGeneral(failedGeneral);
        case DeletionResults.FailedSegments failedSegments -> handleDeletionFailedSegments(failedSegments);
      }
    }

    @Override
    public void onFailure(@Nonnull Throwable t) {
      logger.atSevere().withCause(t).log("Segment deletion threw an unexpected exception");
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
