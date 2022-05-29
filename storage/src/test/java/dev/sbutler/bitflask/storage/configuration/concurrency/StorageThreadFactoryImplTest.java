package dev.sbutler.bitflask.storage.configuration.concurrency;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

public class StorageThreadFactoryImplTest {

  private final StorageThreadFactoryImpl storageThreadFactory = new StorageThreadFactoryImpl();

  @Test
  void newThread() {
    Runnable runnable = mock(Runnable.class);
    Thread thread = storageThreadFactory.newThread(runnable);
    assertTrue(thread.getName().contains("storage-pool"));
  }

}
