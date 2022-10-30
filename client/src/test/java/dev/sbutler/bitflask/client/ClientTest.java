package dev.sbutler.bitflask.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.client.client_processing.ClientProcessor;
import dev.sbutler.bitflask.client.client_processing.ReplClientProcessorService;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

public class ClientTest {

  Injector injector;

  @BeforeEach
  void beforeEach() {
    injector = mock(Injector.class);
  }

  @Test
  void inline() throws Exception {
    try (MockedStatic<SocketChannel> socketChannelMockedStatic = mockStatic(SocketChannel.class);
        MockedStatic<Guice> guiceMockedStatic = mockStatic(Guice.class)) {
      // Arrange
      String[] args = new String[]{"get", "test"};
      SocketChannel socketChannel = mock(SocketChannel.class);
      socketChannelMockedStatic.when(() -> SocketChannel.open(any(SocketAddress.class)))
          .thenReturn(socketChannel);

      guiceMockedStatic.when(() -> Guice.createInjector(any(ClientModule.class)))
          .thenReturn(injector);

      ClientProcessor clientProcessor = mock(ClientProcessor.class);
      doReturn(clientProcessor).when(injector).getInstance(ClientProcessor.class);
      // Act
      Client.main(args);
      // Assert
      verify(clientProcessor, times(1)).processClientInput(any());
    }
  }

  @Test
  void repl() {
    try (MockedStatic<SocketChannel> socketChannelMockedStatic = mockStatic(SocketChannel.class);
        MockedStatic<Guice> guiceMockedStatic = mockStatic(Guice.class)) {
      // Arrange
      String[] args = new String[]{};
      SocketChannel socketChannel = mock(SocketChannel.class);
      socketChannelMockedStatic.when(() -> SocketChannel.open(any(SocketAddress.class)))
          .thenReturn(socketChannel);

      guiceMockedStatic.when(() -> Guice.createInjector(any(ClientModule.class)))
          .thenReturn(injector);

      ReplClientProcessorService replClientProcessorService =
          mock(ReplClientProcessorService.class);
      doReturn(replClientProcessorService).when(injector)
          .getInstance(ReplClientProcessorService.class);
      // Act
      Client.main(args);
      // Assert
      verify(replClientProcessorService, times(1)).run();
    }
  }

  @Test
  void connectionManager_IOException() {
    try (MockedStatic<SocketChannel> socketChannelMockedStatic = mockStatic(SocketChannel.class);
        MockedConstruction<Client> clientMockedConstruction = mockConstruction(Client.class)) {
      // Arrange
      String[] args = new String[]{};
      socketChannelMockedStatic.when(() -> SocketChannel.open(any(SocketAddress.class)))
          .thenThrow(IOException.class);
      // Act
      Client.main(args);
      // Assert
      assertEquals(0, clientMockedConstruction.constructed().size());
    }
  }
}
