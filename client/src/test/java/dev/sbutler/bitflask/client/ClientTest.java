package dev.sbutler.bitflask.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.client.client_processing.ClientProcessor;
import dev.sbutler.bitflask.client.client_processing.ReplClientProcessorService;
import dev.sbutler.bitflask.client.configuration.ClientConfiguration;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

public class ClientTest {

  Client client;
  ClientConfiguration configuration;
  ConnectionManager connectionManager;
  Injector injector;

  @BeforeEach
  void beforeEach() {
    configuration = mock(ClientConfiguration.class);
    connectionManager = mock(ConnectionManager.class);
    injector = mock(Injector.class);
    client = new Client(configuration, connectionManager);
  }

  @Test
  void run_inline() {
    try (MockedStatic<Guice> guiceMockedStatic = mockStatic(Guice.class)) {
      // Arrange
      guiceMockedStatic.when(() -> Guice.createInjector(any(ClientModule.class)))
          .thenReturn(injector);

      ImmutableList<String> clientInput = ImmutableList.of("get", "test");
      doReturn(clientInput).when(configuration).getInlineCmd();

      ClientProcessor clientProcessor = mock(ClientProcessor.class);
      doReturn(clientProcessor).when(injector).getInstance(ClientProcessor.class);
      // Act
      client.run();
      // Assert
      verify(clientProcessor, times(1)).processClientInput(clientInput);
    }
  }

  @Test
  void run_repl() {
    try (MockedStatic<Guice> guiceMockedStatic = mockStatic(Guice.class)) {
      // Arrange
      guiceMockedStatic.when(() -> Guice.createInjector(any(ClientModule.class)))
          .thenReturn(injector);

      doReturn(ImmutableList.of()).when(configuration).getInlineCmd();

      ReplClientProcessorService replClientProcessorService =
          mock(ReplClientProcessorService.class);
      doReturn(replClientProcessorService).when(injector)
          .getInstance(ReplClientProcessorService.class);
      // Act
      client.run();
      // Assert
      verify(replClientProcessorService, times(1)).run();
    }
  }

  @Test
  void main() {
    try (MockedStatic<SocketChannel> socketChannelMockedStatic = mockStatic(SocketChannel.class);
        MockedConstruction<Client> clientMockedConstruction = mockConstruction(Client.class)) {
      // Arrange
      String[] args = new String[]{};
      SocketChannel socketChannel = mock(SocketChannel.class);
      socketChannelMockedStatic.when(() -> SocketChannel.open(any(SocketAddress.class)))
          .thenReturn(socketChannel);
      // Act
      Client.main(args);
      // Assert
      Client mockClient = clientMockedConstruction.constructed().get(0);
      verify(mockClient, times(1)).run();
    }
  }

  @Test
  void main_connectionManager_IOException() {
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
