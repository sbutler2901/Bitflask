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
    try (var currentState = stateManager.getAndLockCurrentState()) {
      if (currentState.getMemtable().getSize() < configurations.getMemtableFlushThresholdMB()) {
        return;
      }
      // flush memtable to segment
      Segment segmentFromMemtable = flushMemtableToSegment(currentState.getMemtable());
      // create new memtable w/ WriteAheadLog
      Memtable newMemtable = createNewMemtable();
      // add memtable to segment level
      SegmentLevelMultiMap newMultiMap = currentState.getSegmentLevelMultiMap().toBuilder()
          .add(segmentFromMemtable)
          .build();

      // update state and release lock
      stateManager.updateCurrentState(newMemtable, newMultiMap);
    }
    // TODO: check segment level threshold, and start compaction. Repeat if necessary for next level
  }

  private Segment flushMemtableToSegment(Memtable memtable) {
    try {
      return segmentFactory.create(memtable.flush());
    } catch (IOException e) {
      throw new StorageCompactionException("Failed to create new Segment from Memtable", e);
    }
  }

  private Memtable createNewMemtable() {
    try {
      return memtableFactory.create();
    } catch (IOException e) {
      throw new StorageCompactionException("Failed creating new Memtable", e);
    }
  }
}
