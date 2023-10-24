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

public class RespNetworkServiceTest {

  @SuppressWarnings("UnstableApiUsage")
  private final ListeningExecutorService executorService =
      spy(TestingExecutors.sameThreadScheduledExecutor());

  private final ServerSocketChannel serverSocketChannel = mock(ServerSocketChannel.class);
  private final RespClientHandlingService.Factory clientHandlingServiceFactory =
      mock(RespClientHandlingService.Factory.class);

  private final RespNetworkService respNetworkService =
      new RespNetworkService(executorService, clientHandlingServiceFactory, serverSocketChannel);

  @Test
  void run() throws Exception {
    // Arrange
    when(serverSocketChannel.isOpen()).thenReturn(true).thenReturn(false);
    SocketChannel socketChannel = mock(SocketChannel.class);
    when(serverSocketChannel.accept()).thenReturn(socketChannel);

    RespClientHandlingService respClientHandlingService = mock(RespClientHandlingService.class);
    when(clientHandlingServiceFactory.create(eq(socketChannel)))
        .thenReturn(respClientHandlingService);
    when(respClientHandlingService.startAsync()).thenReturn(respClientHandlingService);

    // Act
    respNetworkService.run();
    respNetworkService.triggerShutdown();
    // Assert
    verify(respClientHandlingService, times(1)).startAsync();
    verify(respClientHandlingService, times(1)).addListener(any(), any());
    verify(serverSocketChannel, atLeastOnce()).close();
    verify(respClientHandlingService, atLeastOnce()).stopAsync();
  }

  @Test
  void triggerShutdown_close_throwsIOException() throws Exception {
    // Arrange
    doThrow(IOException.class).when(serverSocketChannel).close();
    // Act
    respNetworkService.triggerShutdown();
  }
}
