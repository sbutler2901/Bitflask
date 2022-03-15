package dev.sbutler.bitflask.server.client_handling.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import dev.sbutler.bitflask.resp.network.reader.RespReader;
import dev.sbutler.bitflask.resp.network.writer.RespWriter;
import dev.sbutler.bitflask.server.command_processing.CommandProcessor;
import java.io.InputStream;
import java.io.OutputStream;
import org.junit.jupiter.api.Test;

public class ClientProcessingModuleTest {

  private final ClientProcessingModule clientProcessingModule = new ClientProcessingModule();

  @Test
  void configure() {
    Injector injector = Guice.createInjector(new MockModule(), clientProcessingModule);
    try {
      injector.getBinding(RespReader.class);
      injector.getBinding(RespWriter.class);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  void provideClientRequestHandler() {
    ClientMessageProcessorImpl clientMessageProcessorImpl = mock(ClientMessageProcessorImpl.class);
    ClientMessageProcessor clientMessageProcessor = clientProcessingModule.provideClientRequestHandler(
        clientMessageProcessorImpl);
    assertEquals(clientMessageProcessorImpl, clientMessageProcessor);
    assertInstanceOf(ClientMessageProcessorImpl.class, clientMessageProcessor);
  }

  private static class MockModule extends AbstractModule {

    @Override
    @SuppressWarnings("unchecked")
    protected void configure() {
      super.configure();
      bind(CommandProcessor.class).toProvider(mock(Provider.class));
      bind(InputStream.class).toProvider(mock(Provider.class));
      bind(OutputStream.class).toProvider(mock(Provider.class));
    }
  }
}
