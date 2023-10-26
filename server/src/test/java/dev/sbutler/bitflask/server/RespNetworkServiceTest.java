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
  public void run_acceptNextRespConnection_success() throws Exception {
    when(serverSocketChannel.isOpen()).thenReturn(true).thenReturn(false);
    SocketChannel socketChannel = mock(SocketChannel.class);
    when(serverSocketChannel.accept()).thenReturn(socketChannel);
    RespClientRequestProcessor clientRequestProcessor = mock(RespClientRequestProcessor.class);
    when(clientRequestProcessorFactory.create(any())).thenReturn(clientRequestProcessor);

    try (MockedStatic<RespService> respServiceMockedStatic = mockStatic(RespService.class)) {
      RespService mockRespService = mock(RespService.class);
      respServiceMockedStatic.when(() -> RespService.create(any())).thenReturn(mockRespService);
      respNetworkService.run();
    }

    verify(clientRequestProcessor, atMostOnce()).run();
    verify(serverSocketChannel, atMostOnce()).close();
  }

  @Test
  public void run_acceptNextRespConnection_success_processorFails() throws Exception {
    when(serverSocketChannel.isOpen()).thenReturn(true).thenReturn(false);
    SocketChannel socketChannel = mock(SocketChannel.class);
    when(serverSocketChannel.accept()).thenReturn(socketChannel);
    RespClientRequestProcessor clientRequestProcessor = mock(RespClientRequestProcessor.class);
    when(clientRequestProcessorFactory.create(any())).thenReturn(clientRequestProcessor);
    doThrow(RuntimeException.class).when(clientRequestProcessor).run();

    try (MockedStatic<RespService> respServiceMockedStatic = mockStatic(RespService.class)) {
      RespService mockRespService = mock(RespService.class);
      respServiceMockedStatic.when(() -> RespService.create(any())).thenReturn(mockRespService);
      respNetworkService.run();
    }

    verify(clientRequestProcessor, atMostOnce()).run();
    verify(serverSocketChannel, atMostOnce()).close();
  }

  @Test
  public void run_serverSocketChannelAcceptThrowsIOException() throws Exception {
    when(serverSocketChannel.isOpen()).thenReturn(true);
    when(serverSocketChannel.accept()).thenThrow(IOException.class);

    respNetworkService.run();

    verify(serverSocketChannel, times(2)).close();
  }

  @Test
  public void run_acceptNextRespConnection_respServiceCreateThrowsIOException() throws Exception {
    when(serverSocketChannel.isOpen()).thenReturn(true);
    SocketChannel socketChannel = mock(SocketChannel.class);
    when(serverSocketChannel.accept()).thenReturn(socketChannel);

    try (MockedStatic<RespService> respServiceMockedStatic = mockStatic(RespService.class)) {
      respServiceMockedStatic.when(() -> RespService.create(any())).thenThrow(IOException.class);
      respNetworkService.run();
    }

    verify(serverSocketChannel, times(2)).close();
  }

  @Test
  public void triggerShutdown_close_throwsIOException() throws Exception {
    doThrow(IOException.class).when(serverSocketChannel).close();

    respNetworkService.triggerShutdown();

    verify(serverSocketChannel, atMostOnce()).close();
  }
}
