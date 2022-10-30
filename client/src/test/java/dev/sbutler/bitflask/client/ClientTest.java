package dev.sbutler.bitflask.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.client.client_processing.InlineClientProcessorService;
import dev.sbutler.bitflask.client.client_processing.ReplClientProcessorService;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

public class ClientTest {

  private final Injector injector = mock(Injector.class);
  private final ReplClientProcessorService replProcessor =
      mock(ReplClientProcessorService.class);
  private final ReplClientProcessorService.Factory replFactory =
      mock(ReplClientProcessorService.Factory.class);
  private final InlineClientProcessorService inlineProcessor =
      mock(InlineClientProcessorService.class);
  private final InlineClientProcessorService.Factory inlineFactory =
      mock(InlineClientProcessorService.Factory.class);

  @BeforeEach
  void beforeEach() {
    when(replFactory.create(any())).thenReturn(replProcessor);
    when(inlineFactory.create(any())).thenReturn(inlineProcessor);
    when(injector.getInstance(ReplClientProcessorService.Factory.class))
        .thenReturn(replFactory);
    when(injector.getInstance(InlineClientProcessorService.Factory.class))
        .thenReturn(inlineFactory);
  }

  @Test
  void inline() {
    try (MockedStatic<SocketChannel> socketChannelMockedStatic = mockStatic(SocketChannel.class);
        MockedStatic<Guice> guiceMockedStatic = mockStatic(Guice.class)) {
      // Arrange
      String[] args = new String[]{"get", "test"};
      SocketChannel socketChannel = mock(SocketChannel.class);
      socketChannelMockedStatic.when(() -> SocketChannel.open(any(SocketAddress.class)))
          .thenReturn(socketChannel);

      guiceMockedStatic.when(() -> Guice.createInjector(any(ClientModule.class)))
          .thenReturn(injector);

      // Act
      Client.main(args);
      // Assert
      verify(inlineProcessor, times(1)).run();
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

      // Act
      Client.main(args);
      // Assert
      verify(replProcessor, times(1)).run();
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
