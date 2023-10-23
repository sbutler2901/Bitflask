package dev.sbutler.bitflask.server;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.testing.TestingExecutors;
import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import org.junit.jupiter.api.Test;

public class NetworkServiceTest {

  @SuppressWarnings("UnstableApiUsage")
  private final ListeningExecutorService executorService =
      spy(TestingExecutors.sameThreadScheduledExecutor());

  private final ServerSocketChannel serverSocketChannel = mock(ServerSocketChannel.class);
  private final ClientHandlingService.Factory clientHandlingServiceFactory =
      mock(ClientHandlingService.Factory.class);

  private final NetworkService networkService =
      new NetworkService(executorService, clientHandlingServiceFactory, serverSocketChannel);

  @Test
  void run() throws Exception {
    // Arrange
    when(serverSocketChannel.isOpen()).thenReturn(true).thenReturn(false);
    SocketChannel socketChannel = mock(SocketChannel.class);
    when(serverSocketChannel.accept()).thenReturn(socketChannel);

    ClientHandlingService clientHandlingService = mock(ClientHandlingService.class);
    when(clientHandlingServiceFactory.create(eq(socketChannel))).thenReturn(clientHandlingService);
    when(clientHandlingService.startAsync()).thenReturn(clientHandlingService);

    // Act
    networkService.run();
    networkService.triggerShutdown();
    // Assert
    verify(clientHandlingService, times(1)).startAsync();
    verify(clientHandlingService, times(1)).addListener(any(), any());
    verify(serverSocketChannel, atLeastOnce()).close();
    verify(clientHandlingService, atLeastOnce()).stopAsync();
  }

  @Test
  void triggerShutdown_close_throwsIOException() throws Exception {
    // Arrange
    doThrow(IOException.class).when(serverSocketChannel).close();
    // Act
    networkService.triggerShutdown();
  }
}
