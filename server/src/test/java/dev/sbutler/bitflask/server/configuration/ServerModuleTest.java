package dev.sbutler.bitflask.server.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import dev.sbutler.bitflask.server.network_service.NetworkService;
import dev.sbutler.bitflask.server.network_service.NetworkServiceImpl;
import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
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

  @Test
  void provideServerSocketChannel() throws IOException {
    try (MockedStatic<ServerSocketChannel> serverSocketChannelMockedStatic = mockStatic(
        ServerSocketChannel.class)) {
      ServerSocketChannel mockedServerSocketChannel = mock(ServerSocketChannel.class);
      serverSocketChannelMockedStatic.when(ServerSocketChannel::open)
          .thenReturn(mockedServerSocketChannel);
      ServerSocketChannel serverSocketChannel = serverModule.provideServerSocketChannel(9090);
      assertEquals(mockedServerSocketChannel, serverSocketChannel);
    }
  }

  @Test
  void provideNetworkService() {
    NetworkServiceImpl mockNetworkService = mock(NetworkServiceImpl.class);
    NetworkService networkService = serverModule.provideNetworkService(mockNetworkService);
    assertEquals(mockNetworkService, networkService);
  }
}
