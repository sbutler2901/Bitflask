package dev.sbutler.bitflask.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.client.client_processing.ClientProcessorService;
import org.junit.jupiter.api.Test;
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
        doReturn(mock(ClientProcessorService.class)).when(injector)
            .getInstance(ClientProcessorService.class);
        guiceMockedStatic.when(() -> Guice.createInjector(any(ClientModule.class)))
            .thenReturn(injector);
        // Act
        Client.main(new String[0]);
        ServiceManager serviceManager = serviceManagerMockedConstruction.constructed().get(0);
        // Assert
        verify(injector, times(1)).getInstance(ClientProcessorService.class);
        verify(serviceManager, times(1)).startAsync();
      }
    }
  }
}
