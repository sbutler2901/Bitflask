package dev.sbutler.bitflask.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

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
    injector.getBinding(RespReader.class);
    injector.getBinding(RespWriter.class);
  }

  @Test
  void provideClientConfiguration() {
    // Act
    ClientConfiguration providedConfiguration = clientModule.provideClientConfiguration();
    // Assert
    assertEquals(configuration, providedConfiguration);
  }

  @Test
  void provideInputStream() throws Exception {
    // Arrange
    InputStream inputStream = mock(InputStream.class);
    doReturn(inputStream).when(connectionManager).getInputStream();
    // Act
    InputStream providedInputStream = clientModule.provideInputStream();
    // Assert
    assertEquals(inputStream, providedInputStream);
  }

  @Test
  void provideOutputStream() throws Exception {
    // Arrange
    OutputStream outputStream = mock(OutputStream.class);
    doReturn(outputStream).when(connectionManager).getOutputStream();
    // Act
    OutputStream providedOutputStream = clientModule.provideOutputStream();
    // Assert
    assertEquals(outputStream, providedOutputStream);
  }
}
