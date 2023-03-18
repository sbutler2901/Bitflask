package dev.sbutler.bitflask.storage.lsm;

import dev.sbutler.bitflask.storage.exceptions.StorageLoadException;
import dev.sbutler.bitflask.storage.lsm.memtable.Memtable;
import dev.sbutler.bitflask.storage.lsm.memtable.MemtableLoader;
import dev.sbutler.bitflask.storage.lsm.segment.SegmentLevelMultiMap;
import dev.sbutler.bitflask.storage.lsm.segment.SegmentLevelMultiMapLoader;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import javax.inject.Inject;
import jdk.incubator.concurrent.StructuredTaskScope;

/**
 * Handles loading all necessary resources at start up for the {@link LSMTree}.
 */
final class LSMTreeLoader {

  private final LSMTreeStateManager stateManager;
  private final MemtableLoader memtableLoader;
  private final SegmentLevelMultiMapLoader segmentLevelMultiMapLoader;
  private final ThreadFactory threadFactory;

  @Inject
  LSMTreeLoader(LSMTreeStateManager stateManager, MemtableLoader memtableLoader,
      SegmentLevelMultiMapLoader segmentLevelMultiMapLoader, ThreadFactory threadFactory) {
    this.stateManager = stateManager;
    this.memtableLoader = memtableLoader;
    this.segmentLevelMultiMapLoader = segmentLevelMultiMapLoader;
    this.threadFactory = threadFactory;
  }

  /**
   * Initiate loading of all {@link LSMTree} resources.
   */
  public void load() {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure(
        "lsm-tree-loader", threadFactory)) {
      Future<Memtable> memtable = scope.fork(memtableLoader::load);
      Future<SegmentLevelMultiMap> multiMap = scope.fork(segmentLevelMultiMapLoader::load);

      try {
        scope.join();
        scope.throwIfFailed(StorageLoadException::new);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new StorageLoadException("Failed loading the LSMTree", e);
      }

      try (var ignored = stateManager.getAndLockCurrentState()) {
        stateManager.updateCurrentState(memtable.resultNow(), multiMap.resultNow());
      }
    }
  }
}
