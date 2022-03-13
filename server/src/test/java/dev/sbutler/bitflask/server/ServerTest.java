package dev.sbutler.bitflask.server;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import dev.sbutler.bitflask.server.network_service.NetworkService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServerTest {

  @Test
  void main_success() {
    try (MockedStatic<Guice> guiceMockedStatic = mockStatic(Guice.class)) {
      Injector mockedInjector = mock(Injector.class);
      guiceMockedStatic.when(() -> Guice.createInjector((Module) any())).thenReturn(mockedInjector);
      ExecutorService executorService = mock(ExecutorService.class);
      NetworkService networkService = mock(NetworkService.class);
      doReturn(executorService).when(mockedInjector).getInstance(ExecutorService.class);
      doReturn(networkService).when(mockedInjector).getInstance(NetworkService.class);

      doReturn(mock(Future.class)).when(executorService).submit(networkService);

      Server.main(null);

      verify(executorService, times(1)).submit(networkService);
    }
  }

  @Test
  void main_Exception() throws ExecutionException, InterruptedException {
    try (MockedStatic<Guice> guiceMockedStatic = mockStatic(Guice.class)) {
      Injector mockedInjector = mock(Injector.class);
      guiceMockedStatic.when(() -> Guice.createInjector((Module) any())).thenReturn(mockedInjector);
      ExecutorService executorService = mock(ExecutorService.class);
      NetworkService networkService = mock(NetworkService.class);
      doReturn(executorService).when(mockedInjector).getInstance(ExecutorService.class);
      doReturn(networkService).when(mockedInjector).getInstance(NetworkService.class);

      Future<?> future = mock(Future.class);
      doReturn(future).when(executorService).submit(networkService);

      doThrow(new InterruptedException("test")).when(future).get();

      Server.main(null);

      verify(executorService, times(1)).submit(networkService);
      verify(networkService, times(1)).shutdownAndAwaitTermination();
    }
  }
}
