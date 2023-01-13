package dev.sbutler.bitflask.server;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ServiceManager;
import com.google.common.util.concurrent.ServiceManager.Listener;
import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.server.network_service.NetworkService;
import dev.sbutler.bitflask.storage.StorageService;
import java.nio.channels.ServerSocketChannel;
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
    try (MockedStatic<Guice> guiceMockedStatic = mockStatic(Guice.class);
        MockedStatic<ServerSocketChannel> serverSocketChannelMockedStatic = mockStatic(
            ServerSocketChannel.class)) {
      AtomicReference<ServiceManager> serviceManagerAtomicReference = new AtomicReference<>();
      ArgumentCaptor<Listener> listenerArgumentCaptor = ArgumentCaptor.forClass(Listener.class);
      try (MockedConstruction<ServiceManager> serviceManagerMockedConstruction = mockConstruction(
          ServiceManager.class, (mock, context) -> {
            when(mock.stopAsync()).thenReturn(mock);
            serviceManagerAtomicReference.set(mock);
          })) {
        // Arrange
        Injector injector = mock(Injector.class);
        guiceMockedStatic.when(() -> Guice.createInjector(any(ImmutableSet.class)))
            .thenReturn(injector);

        ServerSocketChannel serverSocketChannel = mock(ServerSocketChannel.class);
        serverSocketChannelMockedStatic.when(ServerSocketChannel::open)
            .thenReturn(serverSocketChannel);

        StorageService storageService = mock(StorageService.class);
        NetworkService.Factory networkServiceFactory = mock(NetworkService.Factory.class);
        when(injector.getInstance(NetworkService.Factory.class)).thenReturn(networkServiceFactory);
        when(injector.getInstance(StorageService.class)).thenReturn(storageService);
        when(injector.getInstance(ListeningExecutorService.class)).thenReturn(
            mock(ListeningExecutorService.class));

        NetworkService networkService = mock(NetworkService.class);
        when(networkServiceFactory.create(serverSocketChannel)).thenReturn(networkService);

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
        verify(injector, times(1)).getInstance(NetworkService.Factory.class);
        verify(injector, times(1)).getInstance(StorageService.class);
        verify(injector, times(1)).getInstance(ListeningExecutorService.class);
        verify(serviceManager, times(1)).startAsync();
      }
    }
  }
}
