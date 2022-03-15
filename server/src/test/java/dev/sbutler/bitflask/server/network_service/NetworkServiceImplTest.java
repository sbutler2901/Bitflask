package dev.sbutler.bitflask.server.network_service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import dev.sbutler.bitflask.server.client_request.ClientRequestHandler;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NetworkServiceImplTest {

  @InjectMocks
  NetworkServiceImpl networkService;

  @Mock
  ExecutorService executorService;
  @Mock
  ServerSocketChannel serverSocketChannel;

  @Test
  void run() throws IOException {
    try (MockedStatic<Guice> guiceMockedStatic = mockStatic(Guice.class)) {
      Injector rootInjector = mock(Injector.class);
      guiceMockedStatic.when(() -> Guice.createInjector((Module) any())).thenReturn(rootInjector);
      Injector childInjector = mock(Injector.class);
      doReturn(childInjector).when(rootInjector).createChildInjector((Module) any());
      doReturn(true).when(serverSocketChannel).isOpen();
      SocketChannel socketChannel = mock(SocketChannel.class);
      when(serverSocketChannel.accept()).thenReturn(socketChannel)
          .thenThrow(new IOException("test: loop termination"));

      ClientRequestHandler mockedClientRequestHandler = mock(ClientRequestHandler.class);
      doReturn(mockedClientRequestHandler).when(childInjector)
          .getInstance(ClientRequestHandler.class);

      networkService.run();

      verify(executorService, times(1)).execute(mockedClientRequestHandler);
    }
  }

  @Test
  void run_ClosedChannelException() throws IOException {
    doThrow(new ClosedChannelException()).when(serverSocketChannel).accept();
    when(serverSocketChannel.isOpen()).thenReturn(true).thenReturn(false);
    networkService.run();
    verify(executorService, times(0)).execute(any(ClientRequestHandler.class));
  }

  @Test
  void close() throws IOException {
    networkService.close();
    verify(serverSocketChannel, times(1)).close();
  }

}
