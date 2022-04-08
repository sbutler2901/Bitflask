package dev.sbutler.bitflask.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;

import dev.sbutler.bitflask.storage.segment.SegmentManagerImpl;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
  void provideExecutorService() {
    try (MockedStatic<Executors> executorsMockedStatic = mockStatic(Executors.class)) {
      ExecutorService mockExecutorService = mock(ExecutorService.class);
      executorsMockedStatic.when(() -> Executors.newFixedThreadPool(anyInt()))
          .thenReturn(mockExecutorService);
      ExecutorService executorService = storageModule.provideExecutorService(4);
      assertEquals(mockExecutorService, executorService);
    }
  }

  @Test
  void provideStorage() {
    try (MockedConstruction<StorageImpl> storageMockedConstruction = mockConstruction(
        StorageImpl.class)) {
      Storage storage = storageModule.provideStorage(mock(SegmentManagerImpl.class));
      Storage mockedStorage = storageMockedConstruction.constructed().get(0);
      assertEquals(mockedStorage, storage);
    }
  }
}
