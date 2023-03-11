package dev.sbutler.bitflask.storage.lsm;

import dev.sbutler.bitflask.storage.exceptions.StorageException;
import dev.sbutler.bitflask.storage.lsm.memtable.Memtable;
import dev.sbutler.bitflask.storage.lsm.memtable.WriteAheadLog;
import dev.sbutler.bitflask.storage.lsm.segment.SegmentLevelMultiMap;
import java.io.IOException;
import javax.inject.Inject;

public final class LSMTreeLoader {

  private final LSMTreeStateManager stateManager;
  private final WriteAheadLog.Factory writeAheadLogFactory;

  @Inject
  LSMTreeLoader(LSMTreeStateManager stateManager, WriteAheadLog.Factory writeAheadLogFactory) {
    this.stateManager = stateManager;
    this.writeAheadLogFactory = writeAheadLogFactory;
  }

  public void load() {
    // TODO: implement proper loading
    Memtable memtable = loadMemtable();
    SegmentLevelMultiMap segmentLevelMultiMap = SegmentLevelMultiMap.create();
    try (var ignored = stateManager.getAndLockCurrentState()) {
      stateManager.updateCurrentState(memtable, segmentLevelMultiMap);
    }
  }

  private Memtable loadMemtable() {
    try {
      WriteAheadLog writeAheadLog = writeAheadLogFactory.create();
      return Memtable.create(writeAheadLog);
    } catch (IOException e) {
      throw new StorageException(e);
    }
  }
}
