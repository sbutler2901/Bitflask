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
import dev.sbutler.bitflask.client.client_processing.ReplClientProcessorService;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
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

  @BeforeEach
  void beforeEach() {
//    when(replFactory.create(any(), anyBoolean())).thenReturn(replProcessor);
//    when(injector.getInstance(ReplClientProcessorService.Factory.class))
//        .thenReturn(replFactory);
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
      setupSocketChannelMock(socketChannel);

      guiceMockedStatic.when(() -> Guice.createInjector(any(ClientModule.class)))
          .thenReturn(injector);

      // Act
      Client.main(args);
      // Assert
      verify(replProcessor, times(1)).run();
    }
  }

  @Test
  void repl() throws Exception {
    try (MockedStatic<SocketChannel> socketChannelMockedStatic = mockStatic(SocketChannel.class);
        MockedStatic<Guice> guiceMockedStatic = mockStatic(Guice.class)) {
      // Arrange
      String[] args = new String[]{};

      SocketChannel socketChannel = mock(SocketChannel.class);
      socketChannelMockedStatic.when(() -> SocketChannel.open(any(SocketAddress.class)))
          .thenReturn(socketChannel);
      setupSocketChannelMock(socketChannel);

      guiceMockedStatic.when(() -> Guice.createInjector(any(ClientModule.class)))
          .thenReturn(injector);

      // Act
      Client.main(args);
      // Assert
      verify(replProcessor, times(1)).run();
    }
  }

  @Test
  void socketChannel_IOException() {
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

  private static void setupSocketChannelMock(SocketChannel socketChannel) throws Exception {
    Socket socket = mock(Socket.class);
    when(socketChannel.socket()).thenReturn(socket);
    InputStream inputStream = mock(InputStream.class);
    OutputStream outputStream = mock(OutputStream.class);
    when(socket.getInputStream()).thenReturn(inputStream);
    when(socket.getOutputStream()).thenReturn(outputStream);
  }
}
