package dev.sbutler.bitflask.client;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.client.configuration.ClientConfiguration;
import dev.sbutler.bitflask.resp.network.RespReader;
import dev.sbutler.bitflask.resp.network.RespWriter;
import java.io.InputStream;
import java.io.OutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ClientModuleTest {

  ClientModule clientModule;
  ClientConfiguration configuration;
  ConnectionManager connectionManager;

  @BeforeEach
  void beforeEach() {
    configuration = mock(ClientConfiguration.class);
    connectionManager = mock(ConnectionManager.class);
    clientModule = ClientModule.create(configuration, connectionManager);
  }

  @Test
  void configure() {
    Injector injector = Guice.createInjector(clientModule);
    // Act / Assert
    injector.getBinding(ClientConfiguration.class);
    injector.getBinding(RespReader.class);
    injector.getBinding(RespWriter.class);
  }

  @Test
  void provideClientConfiguration() {
    // Act
    ClientConfiguration providedConfiguration = clientModule.provideClientConfiguration();
    // Assert
    assertThat(providedConfiguration).isEqualTo(configuration);
  }

  @Test
  void provideRespReader() throws Exception {
    // Arrange
    InputStream inputStream = mock(InputStream.class);
    when(connectionManager.getInputStream()).thenReturn(inputStream);
    // Act
    clientModule.provideRespReader();
    // Assert
    verify(connectionManager, times(1)).getInputStream();
  }

  @Test
  void provideRespWriter() throws Exception {
    // Arrange
    OutputStream outputStream = mock(OutputStream.class);
    when(connectionManager.getOutputStream()).thenReturn(outputStream);
    // Act
    clientModule.provideRespWriter();
    // Assert
    verify(connectionManager, times(1)).getOutputStream();
  }
}
