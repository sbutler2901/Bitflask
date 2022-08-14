package dev.sbutler.bitflask.storage.segment;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.storage.configuration.concurrency.StorageExecutorService;
import dev.sbutler.bitflask.storage.segment.SegmentCompactor.CompactionResults;
import dev.sbutler.bitflask.storage.segment.SegmentDeleter.DeletionResults;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class SegmentManagerService extends AbstractIdleService {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final int DEFAULT_COMPACTION_THRESHOLD_INCREMENT = 3;

  private final ListeningExecutorService executorService;
  private final SegmentFactory segmentFactory;
  private final SegmentCompactorFactory segmentCompactorFactory;
  private final SegmentDeleterFactory segmentDeleterFactory;
  private final SegmentLoader segmentLoader;

  private final AtomicReference<ManagedSegments> managedSegmentsAtomicReference = new AtomicReference<>();

  private final AtomicBoolean compactionActive = new AtomicBoolean(false);
  private final AtomicInteger nextCompactionThreshold = new AtomicInteger(
      DEFAULT_COMPACTION_THRESHOLD_INCREMENT);

  @Inject
  SegmentManagerService(
      @StorageExecutorService ListeningExecutorService executorService,
      SegmentFactory segmentFactory,
      SegmentCompactorFactory segmentCompactorFactory,
      SegmentDeleterFactory segmentDeleterFactory,
      SegmentLoader segmentLoader) {
    this.executorService = executorService;
    this.segmentFactory = segmentFactory;
    this.segmentCompactorFactory = segmentCompactorFactory;
    this.segmentDeleterFactory = segmentDeleterFactory;
    this.segmentLoader = segmentLoader;
  }

  @Override
  public void startUp() throws IOException {
    if (managedSegmentsAtomicReference.get() != null) {
      return;
    }
    ManagedSegments managedSegments = segmentLoader.loadExistingSegments();
    managedSegments.getWritableSegment()
        .registerSizeLimitExceededConsumer(this::segmentSizeLimitExceededConsumer);
    managedSegmentsAtomicReference.set(managedSegments);
  }

  @Override
  public void shutDown() {
    ManagedSegments managedSegments = managedSegmentsAtomicReference.get();
    managedSegments.getWritableSegment().close();
    managedSegments.getFrozenSegments().forEach(Segment::close);
  }

  public ManagedSegments getManagedSegments() {
    return managedSegmentsAtomicReference.get();
  }

  private synchronized void segmentSizeLimitExceededConsumer(Segment segment) {
    if (!segment.equals(managedSegmentsAtomicReference.get().getWritableSegment())) {
      return;
    }
    try {
      createNewWritableSegmentAndUpdateManagedSegments();
      // TODO: simplify compaction activation / handling
      if (shouldInitiateCompaction()) {
        initiateCompaction();
      }
    } catch (IOException e) {
      // TODO: improve error handling
      logger.atSevere().log("Failed to create a new writable segment");
    }
  }

  private synchronized void createNewWritableSegmentAndUpdateManagedSegments() throws IOException {
    logger.atInfo().log("Creating new active segment");
    ManagedSegments currentManagedSegments = managedSegmentsAtomicReference.get();
    Segment newWritableSegment = segmentFactory.createSegment();
    ImmutableList<Segment> newFrozenSegments = new ImmutableList.Builder<Segment>()
        .add(currentManagedSegments.getWritableSegment())
        .addAll(currentManagedSegments.getFrozenSegments())
        .build();

    newWritableSegment.registerSizeLimitExceededConsumer(this::segmentSizeLimitExceededConsumer);
    managedSegmentsAtomicReference.set(
        new ManagedSegments(newWritableSegment, newFrozenSegments));
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
    ListenableFuture<CompactionResults> compactionResults = segmentCompactor.compactSegments();
    Futures.addCallback(compactionResults, new CompactionResultsFutureCallback(), executorService);
  }

  private class CompactionResultsFutureCallback implements FutureCallback<CompactionResults> {

    @Override
    public void onSuccess(CompactionResults result) {
      switch (result) {
        case CompactionResults.Success success -> handleCompactionSuccess(success);
        case CompactionResults.Failed failed -> handleCompactionFailed(failed);
      }
    }

    @Override
    public void onFailure(@Nonnull Throwable t) {
      logger.atSevere().withCause(t).log("Segment compaction threw an unexpected exception");
    }

    private void handleCompactionSuccess(CompactionResults.Success success) {
      logger.atInfo().log("Compaction completed");
      updateAfterCompaction(success.compactedSegments());
      queueSegmentsForDeletion(success.segmentsProvidedForCompaction());
      compactionActive.set(false);
    }

    private void handleCompactionFailed(CompactionResults.Failed failed) {
      logger.atSevere().withCause(failed.failureReason()).log("Compaction failed");
      queueSegmentsForDeletion(failed.failedCompactionSegments());
      compactionActive.set(false);
    }

    private void updateAfterCompaction(ImmutableList<Segment> compactedSegments) {
      synchronized (SegmentManagerService.this) {
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
            new ManagedSegments(currentManagedSegment.getWritableSegment(),
                ImmutableList.copyOf(newFrozenSegments)));
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

  }

  private record DeletionResultsFutureCallback() implements FutureCallback<DeletionResults> {

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
    }

    private void handleDeletionSuccess(DeletionResults.Success results) {
      logger.atInfo().log(buildLogForDeletionSuccess(results.segmentsProvidedForDeletion()));
    }

    private void handleDeletionFailedGeneral(DeletionResults.FailedGeneral results) {
      logger.atSevere().withCause(results.failureReason())
          .log(buildLogForDeletionGeneralFailure(results.segmentsProvidedForDeletion()));
    }

    private void handleDeletionFailedSegments(DeletionResults.FailedSegments results) {
      logger.atSevere().log(buildLogForSegmentsFailure(results.segmentsFailureReasonsMap()));
    }

    private String buildLogForDeletionSuccess(ImmutableList<Segment> segmentsProvidedForDeletion) {
      return "Compacted segments successfully deleted "
          + buildLogForSegmentsToBeDeleted(segmentsProvidedForDeletion);
    }

    private String buildLogForDeletionGeneralFailure(
        ImmutableList<Segment> segmentsProvidedForDeletion) {
      return "Failure to delete compacted segments due to general failure"
          + buildLogForSegmentsToBeDeleted(segmentsProvidedForDeletion);
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

  public record ManagedSegments(Segment writableSegment,
                                ImmutableList<Segment> frozenSegments) {

    public Segment getWritableSegment() {
      return writableSegment;
    }

    public ImmutableList<Segment> getFrozenSegments() {
      return frozenSegments;
    }
  }
}
