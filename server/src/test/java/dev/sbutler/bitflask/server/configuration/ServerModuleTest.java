package dev.sbutler.bitflask.server.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
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
  void provideThreadPoolExecutor() {
    try (MockedStatic<Executors> executorsMockedStatic = mockStatic(Executors.class)) {
      ThreadPoolExecutor mockThreadPoolExecutor = mock(ThreadPoolExecutor.class);
      executorsMockedStatic.when(() -> Executors.newFixedThreadPool(anyInt()))
          .thenReturn(mockThreadPoolExecutor);
      ThreadPoolExecutor threadPoolExecutor = serverModule.provideThreadPoolExecutor(4);
      assertEquals(mockThreadPoolExecutor, threadPoolExecutor);
    }
  }

  @Test
  void provideServerSocket() throws IOException {
    try (MockedConstruction<ServerSocket> serverSocketMockedConstruction = mockConstruction(
        ServerSocket.class)) {
      ServerSocket serverSocket = serverModule.provideServerSocket(9090);
      ServerSocket mockedServerSocket = serverSocketMockedConstruction.constructed().get(0);
      assertEquals(mockedServerSocket, serverSocket);
    }
  }
}
