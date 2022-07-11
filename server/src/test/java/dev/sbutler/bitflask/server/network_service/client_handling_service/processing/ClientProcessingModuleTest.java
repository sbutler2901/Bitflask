package dev.sbutler.bitflask.server.network_service.client_handling_service.processing;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import dev.sbutler.bitflask.resp.network.reader.RespReader;
import dev.sbutler.bitflask.resp.network.writer.RespWriter;
import dev.sbutler.bitflask.server.command_processing_service.CommandProcessingService;
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

  private static class MockModule extends AbstractModule {

    @Override
    @SuppressWarnings("unchecked")
    protected void configure() {
      super.configure();
      bind(CommandProcessingService.class).toProvider(mock(Provider.class));
      bind(InputStream.class).toProvider(mock(Provider.class));
      bind(OutputStream.class).toProvider(mock(Provider.class));
    }
  }
}
