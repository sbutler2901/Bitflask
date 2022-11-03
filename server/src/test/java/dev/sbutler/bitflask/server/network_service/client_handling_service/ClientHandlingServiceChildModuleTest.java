package dev.sbutler.bitflask.server.network_service.client_handling_service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.resp.network.RespReader;
import dev.sbutler.bitflask.resp.network.RespWriter;
import dev.sbutler.bitflask.server.command_processing_service.CommandProcessingService;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SocketChannel;
import javax.inject.Provider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ClientHandlingServiceChildModuleTest {

  @InjectMocks
  private ClientHandlingServiceChildModule clientHandlingServiceChildModule;

  @Mock
  private SocketChannel socketChannel;

  @Test
  void configure() {
    Injector injector = Guice.createInjector(
        new MockModule(),
        clientHandlingServiceChildModule);
    try {
      injector.getBinding(ClientConnectionManager.class);
      injector.getBinding(ClientMessageProcessor.class);
      injector.getBinding(RespReader.class);
      injector.getBinding(RespWriter.class);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  void provideClientConnectionManager() {
    // Act
    ClientConnectionManager clientConnectionManager = clientHandlingServiceChildModule.provideClientConnectionManager();
    // Assert
    assertNotNull(clientConnectionManager);
  }

  @Test
  void provideClientMessageProcessor() {
    // Arrange
    CommandProcessingService commandProcessingService = mock(CommandProcessingService.class);
    RespReader respReader = mock(RespReader.class);
    RespWriter respWriter = mock(RespWriter.class);
    // Act
    ClientMessageProcessor clientMessageProcessor =
        clientHandlingServiceChildModule.provideClientMessageProcessor(
            commandProcessingService,
            respReader,
            respWriter);
    // Act
    assertNotNull(clientMessageProcessor);
  }

  @Test
  void provideRespReader() throws Exception {
    // Arrange
    InputStream inputStream = mock(InputStream.class);
    ClientConnectionManager clientConnectionManager = mock(ClientConnectionManager.class);
    when(clientConnectionManager.getInputStream()).thenReturn(inputStream);
    // Act
    clientHandlingServiceChildModule.provideRespReader(clientConnectionManager);
    // Assert
    verify(clientConnectionManager, times(1)).getInputStream();
  }

  @Test
  void provideRespWriter() throws Exception {
    // Arrange
    OutputStream outputStream = mock(OutputStream.class);
    ClientConnectionManager clientConnectionManager = mock(ClientConnectionManager.class);
    when(clientConnectionManager.getOutputStream()).thenReturn(outputStream);
    // Act
    clientHandlingServiceChildModule.provideRespWriter(clientConnectionManager);
    // Assert
    verify(clientConnectionManager, times(1)).getOutputStream();
  }

  private static class MockModule extends AbstractModule {

    @Override
    @SuppressWarnings("unchecked")
    protected void configure() {
      bind(CommandProcessingService.class).toProvider(mock(Provider.class));
    }
  }
}
