package dev.sbutler.bitflask.server.network_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

public class NetworkServiceModuleTest {

  private final NetworkServiceModule networkServiceModule = new NetworkServiceModule();

  @Test
  void provideNetworkService() {
    NetworkServiceImpl mockNetworkService = mock(NetworkServiceImpl.class);
    NetworkService networkService = networkServiceModule.provideNetworkService(mockNetworkService);
    assertEquals(mockNetworkService, networkService);
  }

  @Test
  void provideServerSocketChannel() throws IOException {
    try (MockedStatic<ServerSocketChannel> serverSocketChannelMockedStatic = mockStatic(
        ServerSocketChannel.class)) {
      ServerSocketChannel mockedServerSocketChannel = mock(ServerSocketChannel.class);
      serverSocketChannelMockedStatic.when(ServerSocketChannel::open)
          .thenReturn(mockedServerSocketChannel);
      ServerSocketChannel serverSocketChannel = networkServiceModule.provideServerSocketChannel(
          9090);
      assertEquals(mockedServerSocketChannel, serverSocketChannel);
    }
  }
}
