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
  private final RespClientService.Factory clientHandlingServiceFactory =
      mock(RespClientService.Factory.class);

  private final RespNetworkService respNetworkService =
      new RespNetworkService(executorService, clientHandlingServiceFactory, serverSocketChannel);

  @Test
  void run() throws Exception {
    // Arrange
    when(serverSocketChannel.isOpen()).thenReturn(true).thenReturn(false);
    SocketChannel socketChannel = mock(SocketChannel.class);
    when(serverSocketChannel.accept()).thenReturn(socketChannel);

    RespClientService respClientService = mock(RespClientService.class);
    when(clientHandlingServiceFactory.create(eq(socketChannel))).thenReturn(respClientService);
    when(respClientService.startAsync()).thenReturn(respClientService);

    // Act
    respNetworkService.run();
    respNetworkService.triggerShutdown();
    // Assert
    verify(respClientService, times(1)).startAsync();
    verify(respClientService, times(1)).addListener(any(), any());
    verify(serverSocketChannel, atLeastOnce()).close();
    verify(respClientService, atLeastOnce()).stopAsync();
  }

  @Test
  void triggerShutdown_close_throwsIOException() throws Exception {
    // Arrange
    doThrow(IOException.class).when(serverSocketChannel).close();
    // Act
    respNetworkService.triggerShutdown();
  }
}
