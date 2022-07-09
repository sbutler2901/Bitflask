package dev.sbutler.bitflask.server.network_service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sbutler.bitflask.server.client_handling_service.ClientRequestHandler;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NetworkServiceTest {

  @InjectMocks
  NetworkService networkService;

  @Mock
  ExecutorService executorService;
  @Mock
  ServerSocketChannel serverSocketChannel;

  @Test
  void run() {
    // TODO: implement
  }

  @Test
  void run_ClosedChannelException() throws Exception {
    doThrow(new ClosedChannelException()).when(serverSocketChannel).accept();
    when(serverSocketChannel.isOpen()).thenReturn(true).thenReturn(false);
    networkService.run();
    verify(executorService, times(0)).execute(any(ClientRequestHandler.class));
  }

  @Test
  void close() throws Exception {
    networkService.close();
    verify(serverSocketChannel, times(1)).close();
  }

}
