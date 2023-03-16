package dev.sbutler.bitflask.storage.lsm;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import dev.sbutler.bitflask.storage.lsm.memtable.MemtableLoader;
import dev.sbutler.bitflask.storage.lsm.segment.SegmentLevelMultiMapLoader;
import java.util.concurrent.ThreadFactory;
import org.junit.jupiter.api.Test;

public class LSMTreeLoaderTest {

  private final LSMTreeStateManager stateManager = mock(LSMTreeStateManager.class);
  private final MemtableLoader memtableLoader = mock(MemtableLoader.class);
  private final SegmentLevelMultiMapLoader segmentLevelMultiMapLoader =
      mock(SegmentLevelMultiMapLoader.class);
  private final ThreadFactory threadFactory = Thread.ofVirtual().factory();

  private final LSMTreeLoader loader = new LSMTreeLoader(stateManager, memtableLoader,
      segmentLevelMultiMapLoader, threadFactory);

  @Test
  public void load_success() {
    loader.load();

    verify(memtableLoader, times(1)).load();
    verify(segmentLevelMultiMapLoader, times(1)).load();

    verify(stateManager, times(1)).getAndLockCurrentState();
    verify(stateManager, times(1)).updateCurrentState(any(), any());
  }
}
