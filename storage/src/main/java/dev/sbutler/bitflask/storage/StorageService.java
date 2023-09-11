package dev.sbutler.bitflask.storage;

import com.google.common.util.concurrent.AbstractIdleService;
import dev.sbutler.bitflask.storage.lsm.LSMTree;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/** Core Storage service that manages initializing and shutdown the storage engine. */
@Singleton
public final class StorageService extends AbstractIdleService {

  private final LSMTree lsmTree;
  private final StorageLoader storageLoader;

  @Inject
  StorageService(LSMTree lsmTree, StorageLoader storageLoader) {
    this.lsmTree = lsmTree;
    this.storageLoader = storageLoader;
  }

  @Override
  protected void startUp() {
    storageLoader.load();
  }

  @Override
  protected void shutDown() {
    lsmTree.close();
  }
}
