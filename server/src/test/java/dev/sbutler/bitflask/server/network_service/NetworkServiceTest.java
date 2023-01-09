package dev.sbutler.bitflask.server.network_service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.testing.TestingExecutors;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NetworkServiceTest {

  @InjectMocks
  private NetworkService networkService;
  @Spy
  @SuppressWarnings("UnstableApiUsage")
  private ListeningExecutorService executorService = TestingExecutors.sameThreadScheduledExecutor();
  @Mock
  private ServerSocketChannel serverSocketChannel;
  @Mock
  private ClientHandlingService.Factory clientHandlingServiceFactory;

  @Test
  void run() throws Exception {
    // Arrange
    when(serverSocketChannel.isOpen()).thenReturn(true).thenReturn(false);
    SocketChannel socketChannel = mock(SocketChannel.class);
    when(serverSocketChannel.accept()).thenReturn(socketChannel);

    ClientHandlingService clientHandlingService = mock(ClientHandlingService.class);
    when(clientHandlingServiceFactory.create(eq(socketChannel)))
        .thenReturn(clientHandlingService);
    when(clientHandlingService.startAsync())
        .thenReturn(clientHandlingService);

    // Act
    networkService.run();
    networkService.triggerShutdown();
    // Assert
    verify(clientHandlingService, times(1)).startAsync();
    verify(clientHandlingService, times(1)).addListener(any(), any());
    verify(serverSocketChannel, times(1)).close();
    verify(clientHandlingService, times(1)).stopAsync();
  }
}
