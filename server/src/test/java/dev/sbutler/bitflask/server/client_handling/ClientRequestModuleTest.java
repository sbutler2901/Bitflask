package dev.sbutler.bitflask.server.client_handling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Binding;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import dev.sbutler.bitflask.resp.network.reader.RespReader;
import dev.sbutler.bitflask.resp.network.writer.RespWriter;
import dev.sbutler.bitflask.server.command_processing.CommandProcessor;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
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
    Binding<?> test = injector.getBinding(ArrayList.class);
    System.out.println("test");
  }

  @Test
  void provideClientRequestHandler() {
    ClientRequestHandlerImpl clientRequestHandlerImpl = mock(ClientRequestHandlerImpl.class);
    ClientRequestHandler clientRequestHandler = clientRequestModule.provideClientRequestHandler(
        clientRequestHandlerImpl);
    assertEquals(clientRequestHandlerImpl, clientRequestHandler);
    assertInstanceOf(ClientRequestHandler.class, clientRequestHandler);
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
      bind(RespReader.class).toProvider(mock(Provider.class));
      bind(RespWriter.class).toProvider(mock(Provider.class));
      bind(CommandProcessor.class).toProvider(mock(Provider.class));
    }
  }
}
