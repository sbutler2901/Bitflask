package dev.sbutler.bitflask.server.client_handling_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import dev.sbutler.bitflask.server.client_handling_service.connection.ClientConnectionManagerImpl;
import dev.sbutler.bitflask.server.client_handling_service.processing.ClientMessageProcessorImpl;
import dev.sbutler.bitflask.server.command_processing_service.CommandProcessingService;
import java.nio.channels.SocketChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ClientRequestModuleTest {

  @InjectMocks
  private ClientRequestModule clientRequestModule;

  @Mock
  private SocketChannel socketChannel;

  @Test
  void configure() {
    Injector injector = Guice.createInjector(new MockModule(), clientRequestModule);
    try {
      injector.getBinding(ClientConnectionManagerImpl.class);
      injector.getBinding(ClientMessageProcessorImpl.class);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  void provideSocketChannel() {
    SocketChannel socketChannel = clientRequestModule.provideSocketChannel();
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