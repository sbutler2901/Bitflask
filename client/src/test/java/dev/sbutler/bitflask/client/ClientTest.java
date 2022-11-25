package dev.sbutler.bitflask.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.inject.ConfigurationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.ProvisionException;
import dev.sbutler.bitflask.client.client_processing.ReplClientProcessorService;
import dev.sbutler.bitflask.resp.network.RespService;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

public class ClientTest {

  private final Injector injector = mock(Injector.class);
  private final ReplClientProcessorService replProcessor =
      mock(ReplClientProcessorService.class);
  private final RespService respService =
      mock(RespService.class);

  @Test
  void success() {
    try (MockedStatic<Guice> guiceMockedStatic = mockStatic(Guice.class)) {
      // Arrange
      when(injector.getInstance(ReplClientProcessorService.class))
          .thenReturn(replProcessor);
      when(injector.getInstance(RespService.class))
          .thenReturn(respService);
      guiceMockedStatic.when(() -> Guice.createInjector(any(ClientModule.class)))
          .thenReturn(injector);
      // Act
      Client.main(new String[0]);
      // Assert
      verify(replProcessor, times(1)).run();
    }
  }

  @Test
  void getInstance_throwsProvisionException() {
    try (MockedStatic<Guice> guiceMockedStatic = mockStatic(Guice.class)) {
      // Arrange
      when(injector.getInstance(ReplClientProcessorService.class))
          .thenThrow(new ProvisionException("test"));
      guiceMockedStatic.when(() -> Guice.createInjector(any(ClientModule.class)))
          .thenReturn(injector);
      // Act
      Client.main(new String[0]);
      // Assert
      verify(replProcessor, times(0)).run();
    }
  }

  @Test
  void getInstance_throwsConfigurationException() {
    try (MockedStatic<Guice> guiceMockedStatic = mockStatic(Guice.class)) {
      // Arrange
      when(injector.getInstance(ReplClientProcessorService.class))
          .thenThrow(new ConfigurationException(ImmutableList.of()));
      guiceMockedStatic.when(() -> Guice.createInjector(any(ClientModule.class)))
          .thenReturn(injector);

      // Act
      Client.main(new String[0]);
      // Assert
      verify(replProcessor, times(0)).run();
    }
  }

  @Test
  void unexpectedFailure() {
    try (MockedStatic<Guice> guiceMockedStatic = mockStatic(Guice.class)) {
      // Arrange
      when(injector.getInstance(ReplClientProcessorService.class))
          .thenReturn(replProcessor);
      when(injector.getInstance(RespService.class))
          .thenReturn(respService);
      guiceMockedStatic.when(() -> Guice.createInjector(any(ClientModule.class)))
          .thenReturn(injector);

      doThrow(new RuntimeException("test")).when(replProcessor).run();

      // Act
      Client.main(new String[0]);
      // Assert
      verify(replProcessor, times(1)).run();
    }
  }

  @Test
  void closeConnection_throwsIoException() throws Exception {
    try (MockedStatic<Guice> guiceMockedStatic = mockStatic(Guice.class)) {
      // Arrange
      when(injector.getInstance(ReplClientProcessorService.class))
          .thenReturn(replProcessor);
      when(injector.getInstance(RespService.class))
          .thenReturn(respService);
      guiceMockedStatic.when(() -> Guice.createInjector(any(ClientModule.class)))
          .thenReturn(injector);

      doThrow(new IOException("test")).when(respService).close();

      // Act
      Client.main(new String[0]);
      // Assert
      verify(replProcessor, times(1)).run();
    }
  }
}
