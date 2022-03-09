package dev.sbutler.bitflask.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

public class StorageModuleTest {

  private final StorageModule storageModule = StorageModule.getInstance();

  @Test
  void provideStorageNumThreads() {
    assertEquals(4, storageModule.provideStorageNumThreads());
  }

  @Test
  void provideThreadPoolExecutor() {
    try (MockedStatic<Executors> executorsMockedStatic = mockStatic(Executors.class)) {
      ThreadPoolExecutor mockThreadPoolExecutor = mock(ThreadPoolExecutor.class);
      executorsMockedStatic.when(() -> Executors.newFixedThreadPool(anyInt()))
          .thenReturn(mockThreadPoolExecutor);
      ThreadPoolExecutor threadPoolExecutor = storageModule.provideThreadPoolExecutor(4);
      assertEquals(mockThreadPoolExecutor, threadPoolExecutor);
    }
  }

  @Test
  void provideStorage() throws IOException {
    try (MockedConstruction<StorageImpl> storageMockedConstruction = mockConstruction(
        StorageImpl.class)) {
      Storage storage = storageModule.provideStorage(mock(ThreadPoolExecutor.class));
      Storage mockedStorage = storageMockedConstruction.constructed().get(0);
      assertEquals(mockedStorage, storage);
    }
  }
}
