package dev.sbutler.bitflask.storage;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.AbstractIdleService;
import dev.sbutler.bitflask.storage.lsm.LSMTree;
import dev.sbutler.bitflask.storage.raft.RaftLoader;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/** Core Storage service that manages initializing and shutdown the storage engine. */
@Singleton
public final class StorageService extends AbstractIdleService {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final LSMTree lsmTree;
  private final StorageLoader storageLoader;
  private final RaftLoader raftLoader;

  @Inject
  StorageService(LSMTree lsmTree, StorageLoader storageLoader, RaftLoader raftLoader) {
    this.lsmTree = lsmTree;
    this.storageLoader = storageLoader;
    this.raftLoader = raftLoader;
  }

  @Override
  protected void startUp() {
    storageLoader.load();
    raftLoader.load();
    logger.atInfo().log("Storage loaded.");
  }

  @Override
  protected void shutDown() {
    lsmTree.close();
    logger.atInfo().log("Storage shutdown");
  }
}
