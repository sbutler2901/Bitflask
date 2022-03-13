package dev.sbutler.bitflask.server.network_service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
import dev.sbutler.bitflask.server.client_processing.ClientRequestHandler;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NetworkServiceTest {

  @InjectMocks
  NetworkService networkService;

  @Mock
  ExecutorService executorService;
  @Mock
  ServerSocket serverSocket;

  @Test
  void run() throws IOException {
    try (MockedStatic<Guice> guiceMockedStatic = mockStatic(Guice.class)) {
      Injector rootInjector = mock(Injector.class);
      guiceMockedStatic.when(() -> Guice.createInjector((Module) any())).thenReturn(rootInjector);
      Injector childInjector = mock(Injector.class);
      doReturn(childInjector).when(rootInjector).createChildInjector((Module) any());
      Socket mockClientSocket = mock(Socket.class);
      when(serverSocket.accept()).thenReturn(mockClientSocket)
          .thenThrow(new IOException("test: loop termination"));

      ClientRequestHandler mockedClientRequestHandler = mock(ClientRequestHandler.class);
      doReturn(mockedClientRequestHandler).when(childInjector)
          .getInstance(ClientRequestHandler.class);

      networkService.run();

      verify(executorService, times(1)).execute(mockedClientRequestHandler);
    }
  }

  @Test
  void run_IOException() throws IOException {
    try (MockedStatic<Guice> guiceMockedStatic = mockStatic(Guice.class)) {
      Injector rootInjector = mock(Injector.class);
      guiceMockedStatic.when(() -> Guice.createInjector((Module) any())).thenReturn(rootInjector);
      doThrow(new IOException("Test: socket accept")).when(serverSocket).accept();

      networkService.run();

      verify(executorService, times(1)).shutdown();
    }
  }

  @Test
  void shutdownAndAwaitTermination() {
    networkService.shutdownAndAwaitTermination();
    verify(executorService, times(1)).shutdown();
    verify(executorService, times(1)).shutdownNow();
  }

  @Test
  void shutdownAndAwaitTermination_InterruptedException() throws InterruptedException {
    doThrow(new InterruptedException("test")).when(executorService)
        .awaitTermination(anyLong(), any());
    networkService.shutdownAndAwaitTermination();
    verify(executorService, times(1)).shutdown();
    verify(executorService, times(1)).shutdownNow();
  }

}
