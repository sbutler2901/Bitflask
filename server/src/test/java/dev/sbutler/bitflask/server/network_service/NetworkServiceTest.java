package dev.sbutler.bitflask.server.network_service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.testing.TestingExecutors;
import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.server.network_service.client_handling_service.ClientHandlingService;
import dev.sbutler.bitflask.server.network_service.client_handling_service.ClientHandlingServiceModule;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
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
  ServerSocketChannel serverSocketChannel;

  @Test
  void run() throws Exception {
    try (MockedStatic<Guice> guiceMockedStatic = mockStatic(Guice.class);
        MockedConstruction<ClientHandlingServiceModule> moduleMockedConstruction = mockConstruction(
            ClientHandlingServiceModule.class)) {
      // Arrange
      when(serverSocketChannel.isOpen()).thenReturn(true).thenReturn(false);
      SocketChannel socketChannel = mock(SocketChannel.class);
      doReturn(socketChannel).when(serverSocketChannel).accept();
      Injector injector = mock(Injector.class);
      guiceMockedStatic.when(() -> Guice.createInjector(any(ClientHandlingServiceModule.class)))
          .thenReturn(injector);
      ClientHandlingService clientHandlingService = mock(ClientHandlingService.class);
      doReturn(clientHandlingService).when(injector).getInstance(ClientHandlingService.class);
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
    doThrow(new ClosedChannelException()).when(serverSocketChannel).accept();
    when(serverSocketChannel.isOpen()).thenReturn(true).thenReturn(false);
    networkService.run();
    verify(executorService, times(0)).execute(any(ClientHandlingService.class));
  }

  @Test
  void shutdown_IOException() throws Exception {
    try (MockedStatic<Guice> guiceMockedStatic = mockStatic(Guice.class);
        MockedConstruction<ClientHandlingServiceModule> moduleMockedConstruction = mockConstruction(
            ClientHandlingServiceModule.class)) {
      // Arrange
      when(serverSocketChannel.isOpen()).thenReturn(true).thenReturn(false);
      SocketChannel socketChannel = mock(SocketChannel.class);
      doReturn(socketChannel).when(serverSocketChannel).accept();
      Injector injector = mock(Injector.class);
      guiceMockedStatic.when(() -> Guice.createInjector(any(ClientHandlingServiceModule.class)))
          .thenReturn(injector);
      ClientHandlingService clientHandlingService = mock(ClientHandlingService.class);
      doReturn(clientHandlingService).when(injector).getInstance(ClientHandlingService.class);
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

}
