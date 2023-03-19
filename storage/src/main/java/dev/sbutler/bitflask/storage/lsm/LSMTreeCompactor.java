package dev.sbutler.bitflask.storage.lsm;

import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.exceptions.StorageCompactionException;
import dev.sbutler.bitflask.storage.lsm.memtable.Memtable;
import dev.sbutler.bitflask.storage.lsm.memtable.MemtableFactory;
import dev.sbutler.bitflask.storage.lsm.segment.Segment;
import dev.sbutler.bitflask.storage.lsm.segment.SegmentFactory;
import dev.sbutler.bitflask.storage.lsm.segment.SegmentLevelMultiMap;
import java.io.IOException;
import javax.inject.Inject;

/**
 * Manages periodically compacting an {@link LSMTree}.
 *
 * <p><b>WARNING</b>: only a single instance of the compactor should be running at any given time.
 */
final class LSMTreeCompactor implements Runnable {

  private final StorageConfigurations configurations;
  private final LSMTreeStateManager stateManager;
  private final MemtableFactory memtableFactory;
  private final SegmentFactory segmentFactory;

  @Inject
  LSMTreeCompactor(
      StorageConfigurations configurations,
      LSMTreeStateManager stateManager,
      MemtableFactory memtableFactory,
      SegmentFactory segmentFactory) {
    this.configurations = configurations;
    this.stateManager = stateManager;
    this.memtableFactory = memtableFactory;
    this.segmentFactory = segmentFactory;
  }

  @Override
  public void run() {
    if (flushMemtable()) {
      compactSegmentLevels();
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
        segmentFromMemtable = segmentFactory.create(currentState.getMemtable());
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
    }
    return true;
  }

  /**
   * Iterates the current segment level's compacting each, if their threshold size has been reached,
   * and updates the {@link LSMTreeStateManager} state accordingly.
   */
  private void compactSegmentLevels() {
    // Assumes another compactor thread will not be altering state.
    SegmentLevelMultiMap segmentLevelMultiMap;
    try (var currentState = stateManager.getCurrentState()) {
      segmentLevelMultiMap = currentState.getSegmentLevelMultiMap();
    }

    int segmentLevel = 0;
    for (; segmentLevelMultiMap.getSizeOfSegmentLevel(segmentLevel)
        >= configurations.getSegmentLevelFlushThresholdBytes();
        segmentLevel++) {
      segmentLevelMultiMap = compactSegmentLevel(segmentLevelMultiMap, segmentLevel);
    }
    // Don't wait for lock if no compaction occurred
    if (segmentLevel == 0) {
      return;
    }

    try (var currentState = stateManager.getAndLockCurrentState()) {
      stateManager.updateCurrentState(currentState.getMemtable(), segmentLevelMultiMap);
    }
  }

  /**
   * Compacts the provided segment level returning the updated
   * {@link dev.sbutler.bitflask.storage.lsm.segment.SegmentLevelMultiMap}.
   */
  private SegmentLevelMultiMap compactSegmentLevel(
      SegmentLevelMultiMap segmentLevelMultiMap, int segmentLevel) {
    // TODO: implement
    return segmentLevelMultiMap;
  }
}
