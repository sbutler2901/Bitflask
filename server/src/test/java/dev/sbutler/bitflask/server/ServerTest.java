package dev.sbutler.bitflask.server;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.util.concurrent.ServiceManager;
import com.google.common.util.concurrent.ServiceManager.Listener;
import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.server.configuration.ServerModule;
import dev.sbutler.bitflask.server.network_service.NetworkService;
import dev.sbutler.bitflask.storage.StorageService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServerTest {

  @Test
  void main() {
    try (MockedStatic<Guice> guiceMockedStatic = mockStatic(Guice.class)) {
      AtomicReference<ServiceManager> serviceManagerAtomicReference = new AtomicReference<>();
      ArgumentCaptor<Listener> listenerArgumentCaptor = ArgumentCaptor.forClass(Listener.class);
      try (MockedConstruction<ServiceManager> serviceManagerMockedConstruction = mockConstruction(
          ServiceManager.class, (mock, context) -> {
            doReturn(mock).when(mock).stopAsync();
            serviceManagerAtomicReference.set(mock);
          })) {
        // Arrange
        Injector injector = mock(Injector.class);
        StorageService storageService = mock(StorageService.class);
        doReturn(storageService).when(injector).getInstance(StorageService.class);
        doReturn(mock(NetworkService.class)).when(injector).getInstance(NetworkService.class);
        doReturn(mock(ExecutorService.class)).when(injector).getInstance(ExecutorService.class);
        guiceMockedStatic.when(() -> Guice.createInjector(any(ServerModule.class)))
            .thenReturn(injector);
        // Act
        Server.main(new String[0]);
        verify(serviceManagerAtomicReference.get()).addListener(listenerArgumentCaptor.capture(),
            any());
        Listener serviceManagerListener = listenerArgumentCaptor.getValue();
        serviceManagerListener.healthy();
        serviceManagerListener.stopped();
        serviceManagerListener.failure(storageService);
        ServiceManager serviceManager = serviceManagerMockedConstruction.constructed().get(0);
        // Assert
        verify(injector, times(1)).getInstance(StorageService.class);
        verify(injector, times(1)).getInstance(NetworkService.class);
        verify(injector, times(1)).getInstance(ExecutorService.class);
        verify(serviceManager, times(1)).startAsync();
      }
    }
  }
}
