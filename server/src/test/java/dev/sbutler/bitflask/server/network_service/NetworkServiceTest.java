package dev.sbutler.bitflask.server.network_service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.testing.TestingExecutors;
import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.server.configuration.ServerConfigurations;
import dev.sbutler.bitflask.server.network_service.client_handling_service.ClientHandlingService;
import dev.sbutler.bitflask.server.network_service.client_handling_service.ClientHandlingServiceChildModule;
import dev.sbutler.bitflask.server.network_service.client_handling_service.ClientHandlingServiceParentModule;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NetworkServiceTest {

  @InjectMocks
  NetworkService networkService;
  @Spy
  @SuppressWarnings("UnstableApiUsage")
  ListeningExecutorService executorService = TestingExecutors.sameThreadScheduledExecutor();
  @Mock
  ServerConfigurations serverConfigurations;
  @Mock
  ServerSocketChannel serverSocketChannel;

  @BeforeEach
  void beforeEach() {
    doReturn(9090).when(serverConfigurations).getPort();
  }

  @Test
  void startUp() throws Exception {
    try (MockedStatic<ServerSocketChannel> serverSocketChannelMockedStatic =
        mockStatic(ServerSocketChannel.class)) {
      // Arrange
      ServerSocketChannel serverSocketChannel = mock(ServerSocketChannel.class);
      serverSocketChannelMockedStatic.when(ServerSocketChannel::open)
          .thenReturn(serverSocketChannel);
      // Act
      networkService.startUp();
      // Assert
      verify(serverSocketChannel, times(1)).bind(any());
    }
  }

  @Test
  void run() throws Exception {
    try (MockedStatic<Guice> guiceMockedStatic = mockStatic(Guice.class);
        MockedStatic<ServerSocketChannel> serverSocketChannelMockedStatic =
            mockStatic(ServerSocketChannel.class)) {
      // Arrange
      serverSocketChannelMockedStatic.when(ServerSocketChannel::open)
          .thenReturn(serverSocketChannel);
      when(serverSocketChannel.isOpen()).thenReturn(true).thenReturn(false);
      SocketChannel socketChannel = mock(SocketChannel.class);
      doReturn(socketChannel).when(serverSocketChannel).accept();
      setupSocketChannelMock(socketChannel);

      Injector parentInjector = mock(Injector.class);
      guiceMockedStatic.when(
              () -> Guice.createInjector(any(ClientHandlingServiceParentModule.class)))
          .thenReturn(parentInjector);
      Injector childInjector = mock(Injector.class);
      doReturn(childInjector).when(parentInjector).createChildInjector(any(
          ClientHandlingServiceChildModule.class));
      ClientHandlingService clientHandlingService = mock(ClientHandlingService.class);
      doReturn(clientHandlingService).when(childInjector).getInstance(ClientHandlingService.class);
      // Act
      networkService.startUp();
      networkService.run();
      networkService.triggerShutdown();
      // Assert
      verify(serverSocketChannel, times(1)).close();
      verify(clientHandlingService, times(1)).close();
    }
  }

  @Test
  void run_ClosedChannelException() throws Exception {
    try (MockedStatic<ServerSocketChannel> serverSocketChannelMockedStatic =
        mockStatic(ServerSocketChannel.class)) {
      // Arrange
      serverSocketChannelMockedStatic.when(ServerSocketChannel::open)
          .thenReturn(serverSocketChannel);
      doThrow(new ClosedChannelException()).when(serverSocketChannel).accept();
      when(serverSocketChannel.isOpen()).thenReturn(true).thenReturn(false);
      // Act
      networkService.startUp();
      networkService.run();
      // Assert
      verify(executorService, times(0)).execute(any(ClientHandlingService.class));
    }
  }

  @Test
  void shutdown_IOException() throws Exception {
    try (MockedStatic<Guice> guiceMockedStatic = mockStatic(Guice.class);
        MockedStatic<ServerSocketChannel> serverSocketChannelMockedStatic =
            mockStatic(ServerSocketChannel.class)) {
      // Arrange
      serverSocketChannelMockedStatic.when(ServerSocketChannel::open)
          .thenReturn(serverSocketChannel);
      when(serverSocketChannel.isOpen()).thenReturn(true).thenReturn(false);
      SocketChannel socketChannel = mock(SocketChannel.class);
      doReturn(socketChannel).when(serverSocketChannel).accept();
      setupSocketChannelMock(socketChannel);

      Injector parentInjector = mock(Injector.class);
      guiceMockedStatic.when(
              () -> Guice.createInjector(any(ClientHandlingServiceParentModule.class)))
          .thenReturn(parentInjector);
      Injector childInjector = mock(Injector.class);
      doReturn(childInjector).when(parentInjector).createChildInjector(any(
          ClientHandlingServiceChildModule.class));
      ClientHandlingService clientHandlingService = mock(ClientHandlingService.class);
      doReturn(clientHandlingService).when(childInjector).getInstance(ClientHandlingService.class);

      doThrow(IOException.class).when(serverSocketChannel).close();
      // Act
      networkService.startUp();
      networkService.run();
      networkService.triggerShutdown();
      // Assert
      verify(serverSocketChannel, times(1)).close();
      verify(clientHandlingService, times(1)).close();
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
