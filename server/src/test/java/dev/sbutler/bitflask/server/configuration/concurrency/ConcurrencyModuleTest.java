package dev.sbutler.bitflask.server.configuration.concurrency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

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
      ExecutorService mockExecutorService = mock(ExecutorService.class);
      VirtualThreadFactory virtualThreadFactory = mock(VirtualThreadFactory.class);
      executorsMockedStatic.when(
              () -> Executors.newThreadPerTaskExecutor(any(ThreadFactory.class)))
          .thenReturn(mockExecutorService);
      ExecutorService executorService = concurrencyModule.provideExecutorService(
          virtualThreadFactory);
      assertEquals(mockExecutorService, executorService);
    }
  }
}
