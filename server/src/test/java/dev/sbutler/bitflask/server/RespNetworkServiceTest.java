package dev.sbutler.bitflask.server;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.testing.TestingExecutors;
import dev.sbutler.bitflask.resp.network.RespService;
import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

public class RespNetworkServiceTest {

  @SuppressWarnings("UnstableApiUsage")
  private final ListeningExecutorService executorService =
      spy(TestingExecutors.sameThreadScheduledExecutor());

  private final ServerSocketChannel serverSocketChannel = mock(ServerSocketChannel.class);
  private final RespClientRequestProcessor.Factory clientRequestProcessorFactory =
      mock(RespClientRequestProcessor.Factory.class);

  private final RespNetworkService respNetworkService =
      new RespNetworkService(executorService, clientRequestProcessorFactory, serverSocketChannel);

  @Test
  void run() throws Exception {
    when(serverSocketChannel.isOpen()).thenReturn(true).thenReturn(false);
    SocketChannel socketChannel = mock(SocketChannel.class);
    when(serverSocketChannel.accept()).thenReturn(socketChannel);
    RespClientRequestProcessor clientRequestProcessor = mock(RespClientRequestProcessor.class);
    when(clientRequestProcessorFactory.create(any())).thenReturn(clientRequestProcessor);

    RespService respService = mock(RespService.class);
    try (MockedStatic<RespService> respServiceMockedStatic = mockStatic(RespService.class)) {
      respServiceMockedStatic.when(() -> RespService.create(any())).thenReturn(respService);
      respNetworkService.run();
    }

    verify(serverSocketChannel, atLeastOnce()).close();
  }

  @Test
  void triggerShutdown_close_throwsIOException() throws Exception {
    // Arrange
    doThrow(IOException.class).when(serverSocketChannel).close();
    // Act
    respNetworkService.triggerShutdown();
  }
}
