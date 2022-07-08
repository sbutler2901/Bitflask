package dev.sbutler.bitflask.server;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import dev.sbutler.bitflask.server.network_service.NetworkServiceImpl;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServerTest {

  @InjectMocks
  Server server;
  @Mock
  ExecutorService executorService;
  @Mock
  NetworkServiceImpl networkService;

  @Test
  void main() {
    try (MockedStatic<Guice> guiceMockedStatic = mockStatic(Guice.class)) {
      Injector mockedInjector = mock(Injector.class);
      guiceMockedStatic.when(() -> Guice.createInjector((Module) any())).thenReturn(mockedInjector);
      doReturn(server).when(mockedInjector).getInstance(Server.class);

      doReturn(mock(Future.class)).when(executorService).submit(networkService);

      Server.main(null);

      verify(executorService, times(1)).submit(networkService);
    }
  }

  @Test
  void run() {
    doReturn(mock(Future.class)).when(executorService).submit(networkService);
    server.run();
    verify(executorService, times(1)).submit(networkService);
  }

  @Test
  void run_Exception() throws ExecutionException, InterruptedException {
    Future<?> future = mock(Future.class);
    doReturn(future).when(executorService).submit(networkService);
    doThrow(new InterruptedException("test")).when(future).get();
    server.run();
    verify(executorService, times(1)).submit(networkService);
  }

  @Test
  void shutdown() throws InterruptedException, IOException {
    server.shutdown();
    verify(networkService, times(1)).close();
    verify(executorService, times(1)).shutdownNow();
    verify(executorService, times(1)).awaitTermination(anyLong(), any());
  }

  @Test
  void shutdown_IOException() throws IOException, InterruptedException {
    doThrow(new IOException("test")).when(networkService).close();
    server.shutdown();
    verify(networkService, times(1)).close();
    verify(executorService, times(1)).shutdownNow();
    verify(executorService, times(1)).awaitTermination(anyLong(), any());
  }

  @Test
  void shutdown_InterruptedException() throws InterruptedException, IOException {
    doThrow(new InterruptedException("test")).when(executorService)
        .awaitTermination(anyLong(), any());
    server.shutdown();
    verify(networkService, times(1)).close();
    verify(executorService, times(2)).shutdownNow();
    verify(executorService, times(1)).awaitTermination(anyLong(), any());
  }
}
