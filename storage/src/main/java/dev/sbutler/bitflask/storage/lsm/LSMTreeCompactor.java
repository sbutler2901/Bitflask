package dev.sbutler.bitflask.storage.lsm;

import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.exceptions.StorageCompactionException;
import dev.sbutler.bitflask.storage.lsm.memtable.Memtable;
import dev.sbutler.bitflask.storage.lsm.memtable.MemtableFactory;
import dev.sbutler.bitflask.storage.lsm.segment.Segment;
import dev.sbutler.bitflask.storage.lsm.segment.SegmentFactory;
import dev.sbutler.bitflask.storage.lsm.segment.SegmentLevelCompactor;
import dev.sbutler.bitflask.storage.lsm.segment.SegmentLevelMultiMap;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import javax.inject.Inject;

/**
 * Manages periodically compacting an {@link LSMTree}.
 *
 * <p><b>WARNING</b>: only a single instance of the compactor should be running at any given time.
 */
final class LSMTreeCompactor implements Runnable {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final StorageConfigurations configurations;
  private final LSMTreeStateManager stateManager;
  private final MemtableFactory memtableFactory;
  private final SegmentFactory segmentFactory;
  private final SegmentLevelCompactor segmentLevelCompactor;

  @Inject
  LSMTreeCompactor(
      StorageConfigurations configurations,
      LSMTreeStateManager stateManager,
      SegmentLevelCompactor segmentLevelCompactor,
      MemtableFactory memtableFactory,
      SegmentFactory segmentFactory) {
    this.configurations = configurations;
    this.stateManager = stateManager;
    this.segmentLevelCompactor = segmentLevelCompactor;
    this.memtableFactory = memtableFactory;
    this.segmentFactory = segmentFactory;
  }

  @Override
  public void run() {
    logger.atInfo().log("Starting compaction");

    Instant startInstant = Instant.now();
    if (flushMemtable()) {
      int numLevelsCompacted = compactSegmentLevels();
      logger.atInfo()
          .log("Compacted Memtable & [%d] segment levels in [%d]millis", numLevelsCompacted,
              Duration.between(startInstant, Instant.now()));
    } else {
      logger.atInfo().log("Ending compaction without flushing Memtable");
    }
  }

  /**
   * Returns true if the current {@link Memtable} was flushed to a {@link Segment}.
   */
  boolean flushMemtable() {
    try (var currentState = stateManager.getAndLockCurrentState()) {
      if (currentState.getMemtable().getNumBytesSize()
          < configurations.getMemtableFlushThresholdBytes()) {
        return false;
      }

      Segment segmentFromMemtable;
      try {
        segmentFromMemtable = segmentFactory.create(
            currentState.getMemtable().flush(),
            0,
            currentState.getMemtable().getNumBytesSize());
      } catch (IOException e) {
        throw new StorageCompactionException("Failed to create new Segment from Memtable", e);
      }

      Memtable newMemtable;
      try {
        newMemtable = memtableFactory.create();
      } catch (IOException e) {
        throw new StorageCompactionException("Failed creating new Memtable", e);
      }

      // add memtable to segment level
      SegmentLevelMultiMap newMultiMap = currentState.getSegmentLevelMultiMap().toBuilder()
          .add(segmentFromMemtable)
          .build();

      // update state and release lock
      stateManager.updateCurrentState(newMemtable, newMultiMap);

      logger.atInfo()
          .log("Flushed Memtable to Segment [%d]", segmentFromMemtable.getSegmentNumber());
    }
    return true;
  }

  /**
   * Iterates the current segment level's compacting each, if their threshold size has been reached,
   * and updates the {@link LSMTreeStateManager} state accordingly.
   *
   * @return the number of segment levels compacted
   */
  int compactSegmentLevels() {
    // Assumes another compactor thread will not be altering state.
    SegmentLevelMultiMap segmentLevelMultiMap;
    try (var currentState = stateManager.getCurrentState()) {
      segmentLevelMultiMap = currentState.getSegmentLevelMultiMap();
    }

    int segmentLevel = 0;
    for (; segmentLevelMultiMap.getNumBytesSizeOfSegmentLevel(segmentLevel)
        >= configurations.getSegmentLevelFlushThresholdBytes();
        segmentLevel++) {
      segmentLevelMultiMap = segmentLevelCompactor
          .compactSegmentLevel(segmentLevelMultiMap, segmentLevel);
      logger.atInfo().log("Compacted segment level [%d]", segmentLevel);
    }

    // Only wait for lock if compaction occurred
    if (segmentLevel > 0) {
      try (var currentState = stateManager.getAndLockCurrentState()) {
        stateManager.updateCurrentState(currentState.getMemtable(), segmentLevelMultiMap);
      }
    }
    return segmentLevel;
  }
}
