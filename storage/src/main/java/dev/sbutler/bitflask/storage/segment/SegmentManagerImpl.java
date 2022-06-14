package dev.sbutler.bitflask.storage.segment;

import dev.sbutler.bitflask.storage.configuration.concurrency.StorageExecutorService;
import dev.sbutler.bitflask.storage.configuration.logging.InjectStorageLogger;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import org.slf4j.Logger;

class SegmentManagerImpl implements SegmentManager {

  private static final int DEFAULT_COMPACTION_THRESHOLD_INCREMENT = 3;

  @InjectStorageLogger
  static Logger logger;

  private final ExecutorService executorService;
  private final SegmentFactory segmentFactory;

  private List<Segment> frozenSegments;
  private Segment activeSegment;

  private SegmentCompactorImpl segmentCompactor;
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
    return segmentCompactor == null && frozenSegments.size() >= nextCompactionThreshold.get();
  }

  private void initiateCompaction() {
    logger.info("Initiating Compaction");
    segmentCompactor = new SegmentCompactorImpl(segmentFactory, frozenSegments);
    CompletableFuture
        .supplyAsync(this::performCompaction, executorService)
        .whenComplete((result, e) -> {
          if (e != null) {
            finalizeAfterCompactionFailure(e);
          }
        })
        .thenAccept(this::updateAfterCompaction)
        // todo: handle segments failed to be closed
        .thenRunAsync(segmentCompactor::closeAndDeleteSegments, executorService)
        .thenRun(this::finalizeAfterCompaction);
  }

  private List<Segment> performCompaction() {
    logger.info("Performing compaction");
    try {
      return segmentCompactor.compactSegments();
    } catch (IOException e) {
      throw new CompletionException(e);
    }
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

  private void finalizeAfterCompaction() {
    segmentCompactor = null;
    logger.info("Compaction completed");
  }

  private void finalizeAfterCompactionFailure(Throwable compactionThrowable) {
    segmentCompactor = null;
    logger.error("Failed to compact segments", compactionThrowable.getCause());
  }

}
