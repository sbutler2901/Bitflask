package dev.sbutler.bitflask.storage.lsm;

import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.exceptions.StorageCompactionException;
import dev.sbutler.bitflask.storage.lsm.memtable.Memtable;
import dev.sbutler.bitflask.storage.lsm.memtable.MemtableFactory;
import dev.sbutler.bitflask.storage.lsm.segment.Segment;
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

  @Inject
  LSMTreeCompactor(
      StorageConfigurations configurations,
      LSMTreeStateManager stateManager,
      MemtableFactory memtableFactory) {
    this.configurations = configurations;
    this.stateManager = stateManager;
    this.memtableFactory = memtableFactory;
  }

  @Override
  public void run() {
    try (var currentState = stateManager.getAndLockCurrentState()) {
      if (currentState.getMemtable().getSize() < configurations.getMemtableFlushThresholdMB()) {
        return;
      }
      // flush memtable to segment
      Segment memtableFlushed = flushMemtable(currentState.getMemtable());
      // create new memtable w/ WriteAheadLog
      Memtable newMemtable;
      try {
        newMemtable = memtableFactory.create();
      } catch (IOException e) {
        throw new StorageCompactionException("Failed creating new Memtable", e);
      }

      // add memtable to segment level
      SegmentLevelMultiMap newMultiMap = currentState.getSegmentLevelMultiMap().toBuilder()
          .put(0, memtableFlushed)
          .build();

      // update state and release lock
      stateManager.updateCurrentState(newMemtable, newMultiMap);
    }
    // TODO: check segment level threshold, and start compaction. Repeat if necessary for next level
  }

  private Segment flushMemtable(Memtable memtable) {
    // TODO: implement
    return null;
  }
}
