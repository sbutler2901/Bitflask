package dev.sbutler.bitflask.storage.lsm;

import dev.sbutler.bitflask.storage.lsm.memtable.Memtable;
import dev.sbutler.bitflask.storage.lsm.memtable.MemtableLoader;
import dev.sbutler.bitflask.storage.lsm.segment.SegmentLevelMultiMap;
import dev.sbutler.bitflask.storage.lsm.segment.SegmentLevelMultiMapLoader;
import javax.inject.Inject;

public final class LSMTreeLoader {

  private final LSMTreeStateManager stateManager;
  private final MemtableLoader memtableLoader;
  private final SegmentLevelMultiMapLoader segmentLevelMultiMapLoader;

  @Inject
  LSMTreeLoader(LSMTreeStateManager stateManager, MemtableLoader memtableLoader,
      SegmentLevelMultiMapLoader segmentLevelMultiMapLoader) {
    this.stateManager = stateManager;
    this.memtableLoader = memtableLoader;
    this.segmentLevelMultiMapLoader = segmentLevelMultiMapLoader;
  }

  public void load() {
    Memtable memtable = memtableLoader.load();
    SegmentLevelMultiMap segmentLevelMultiMap = segmentLevelMultiMapLoader.load();
    try (var ignored = stateManager.getAndLockCurrentState()) {
      stateManager.updateCurrentState(memtable, segmentLevelMultiMap);
    }
  }
}
