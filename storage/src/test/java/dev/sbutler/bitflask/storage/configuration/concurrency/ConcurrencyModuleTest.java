package dev.sbutler.bitflask.storage.configuration.concurrency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.google.common.util.concurrent.ListeningExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

public class ConcurrencyModuleTest {

  private final ConcurrencyModule concurrencyModule = ConcurrencyModule.getInstance();

  @Test
  void provideExecutorService() {
    try (MockedStatic<Executors> executorsMockedStatic = mockStatic(Executors.class)) {
      ListeningExecutorService mockExecutorService = mock(ListeningExecutorService.class);
      StorageThreadFactory storageThreadFactory = mock(StorageThreadFactory.class);
      executorsMockedStatic.when(
              () -> Executors.newThreadPerTaskExecutor(any(ThreadFactory.class)))
          .thenReturn(mockExecutorService);
      ExecutorService executorService = concurrencyModule.provideExecutorService(
          storageThreadFactory);
      assertEquals(mockExecutorService, executorService);
    }
  }
}
