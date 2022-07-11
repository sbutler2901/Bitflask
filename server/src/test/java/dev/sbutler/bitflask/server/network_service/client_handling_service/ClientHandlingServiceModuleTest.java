package dev.sbutler.bitflask.server.network_service.client_handling_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import dev.sbutler.bitflask.server.command_processing_service.CommandProcessingService;
import dev.sbutler.bitflask.server.network_service.client_handling_service.connection.ClientConnectionManager;
import dev.sbutler.bitflask.server.network_service.client_handling_service.processing.ClientMessageProcessor;
import java.nio.channels.SocketChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ClientHandlingServiceModuleTest {

  @InjectMocks
  private ClientHandlingServiceModule clientHandlingServiceModule;

  @Mock
  private SocketChannel socketChannel;

  @Test
  void configure() {
    Injector injector = Guice.createInjector(new MockModule(), clientHandlingServiceModule);
    try {
      injector.getBinding(ClientConnectionManager.class);
      injector.getBinding(ClientMessageProcessor.class);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  void provideSocketChannel() {
    SocketChannel socketChannel = clientHandlingServiceModule.provideSocketChannel();
    assertEquals(this.socketChannel, socketChannel);
  }

  private static class MockModule extends AbstractModule {

    @Override
    @SuppressWarnings("unchecked")
    protected void configure() {
      bind(CommandProcessingService.class).toProvider(mock(Provider.class));
    }
  }
}
