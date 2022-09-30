package dev.sbutler.bitflask.storage.configuration.concurrency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.lang.Thread.State;
import org.junit.jupiter.api.Test;

public class StorageThreadFactoryTest {

  private final StorageThreadFactory storageThreadFactory = new StorageThreadFactory();

  @Test
  void newThread() {
    Runnable runnable = mock(Runnable.class);
    Thread thread = storageThreadFactory.newThread(runnable);
    assertTrue(thread.isVirtual());
    assertEquals(thread.getState(), State.NEW);
    assertTrue(thread.getName().contains("storage-pool"));
  }

}
