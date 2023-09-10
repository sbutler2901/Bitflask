package dev.sbutler.bitflask.storage;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import dev.sbutler.bitflask.storage.lsm.LSMTree;
import dev.sbutler.bitflask.storage.raft.RaftLoader;
import org.junit.jupiter.api.Test;

class StorageServiceTest {

  private final LSMTree lsmTree = mock(LSMTree.class);
  private final StorageLoader storageLoader = mock(StorageLoader.class);
  private final RaftLoader raftLoader = mock(RaftLoader.class);

  private final StorageService storageService =
      new StorageService(lsmTree, storageLoader, raftLoader);

  @Test
  public void startUp() {
    storageService.startUp();

    verify(storageLoader, times(1)).load();
    verify(raftLoader, times(1)).load();
  }

  @Test
  public void shutdown() {
    storageService.shutDown();
    verify(lsmTree, times(1)).close();
  }
}
