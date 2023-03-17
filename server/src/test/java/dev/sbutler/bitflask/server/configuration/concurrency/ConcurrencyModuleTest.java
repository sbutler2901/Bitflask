package dev.sbutler.bitflask.server.configuration.concurrency;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.common.concurrency.VirtualThreadFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

public class ConcurrencyModuleTest {

  private final ConcurrencyModule concurrencyModule = new ConcurrencyModule();

  @Test
  void provideExecutorService() {
    try (MockedStatic<Executors> executorsMockedStatic = mockStatic(Executors.class)) {
      ListeningExecutorService mockExecutorService = mock(ListeningExecutorService.class);
      VirtualThreadFactory virtualThreadFactory = mock(VirtualThreadFactory.class);
      executorsMockedStatic.when(
              () -> Executors.newThreadPerTaskExecutor(any(ThreadFactory.class)))
          .thenReturn(mockExecutorService);

      ListeningExecutorService executorService = concurrencyModule.provideExecutorService(
          virtualThreadFactory);

      assertThat(executorService).isEqualTo(mockExecutorService);
    }
  }

  @Test
  void provideThreadFactory() {
    VirtualThreadFactory virtualThreadFactory = new VirtualThreadFactory();

    ThreadFactory threadFactory = concurrencyModule.provideThreadFactory(virtualThreadFactory);

    assertThat(threadFactory).isEqualTo(virtualThreadFactory);
  }
}
