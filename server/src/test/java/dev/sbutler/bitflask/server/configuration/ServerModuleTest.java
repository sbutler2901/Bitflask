package dev.sbutler.bitflask.server.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

public class ServerModuleTest {

  private final ServerModule serverModule = ServerModule.getInstance();

  @Test
  void provideServerPort() {
    assertEquals(9090, serverModule.provideServerPort());
  }

  @Test
  void provideServerNumThreads() {
    assertEquals(4, serverModule.provideServerNumThreads());
  }

  @Test
  void provideExecutorService() {
    try (MockedStatic<Executors> executorsMockedStatic = mockStatic(Executors.class)) {
      ExecutorService mockExecutorService = mock(ExecutorService.class);
      executorsMockedStatic.when(() -> Executors.newFixedThreadPool(anyInt()))
          .thenReturn(mockExecutorService);
      ExecutorService executorService = serverModule.provideExecutorService(4);
      assertEquals(mockExecutorService, executorService);
    }
  }


}
