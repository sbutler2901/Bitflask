package dev.sbutler.bitflask.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.util.concurrent.ServiceManager;
import com.google.common.util.concurrent.ServiceManager.Listener;
import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.client.client_processing.ReplClientProcessorService;
import dev.sbutler.bitflask.client.connection.ConnectionManager;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

public class ClientTest {

  @Test
  void main() {
    try (MockedStatic<Guice> guiceMockedStatic = mockStatic(Guice.class)) {
      try (MockedConstruction<ServiceManager> serviceManagerMockedConstruction = mockConstruction(
          ServiceManager.class)) {
        // Arrange
        Injector injector = mock(Injector.class);
        doReturn(mock(ReplClientProcessorService.class)).when(injector)
            .getInstance(ReplClientProcessorService.class);
        guiceMockedStatic.when(() -> Guice.createInjector(any(ClientModule.class)))
            .thenReturn(injector);
        // Act
        Client.main(new String[0]);
        ServiceManager serviceManager = serviceManagerMockedConstruction.constructed().get(0);
        doReturn(serviceManager).when(serviceManager).stopAsync();
        // Assert
        verify(injector, times(1)).getInstance(ReplClientProcessorService.class);
        verify(serviceManager, times(1)).startAsync();
      }
    }
  }

  @Test
  void main_serviceFailure() throws Exception {
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
        ReplClientProcessorService replClientProcessorService = mock(
            ReplClientProcessorService.class);
        doReturn(replClientProcessorService).when(injector)
            .getInstance(ReplClientProcessorService.class);
        ConnectionManager connectionManager = mock(ConnectionManager.class);
        doThrow(IOException.class).when(connectionManager).close();
        doReturn(connectionManager).when(injector)
            .getInstance(ConnectionManager.class);
        guiceMockedStatic.when(() -> Guice.createInjector(any(ClientModule.class)))
            .thenReturn(injector);
        // Act
        Client.main(new String[0]);
        verify(serviceManagerAtomicReference.get()).addListener(listenerArgumentCaptor.capture(),
            any());
        ServiceManager serviceManager = serviceManagerMockedConstruction.constructed().get(0);
        doReturn(serviceManager).when(serviceManager).stopAsync();
        doThrow(TimeoutException.class).when(serviceManager).awaitStopped(anyLong(), any());
        Listener serviceManagerListener = listenerArgumentCaptor.getValue();
        serviceManagerListener.failure(replClientProcessorService);
        // Assert
        verify(injector, times(1)).getInstance(ReplClientProcessorService.class);
        verify(serviceManager, times(1)).startAsync();
      }
    }
  }
}
