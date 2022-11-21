package dev.sbutler.bitflask.client.client_processing;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.client.client_processing.output.OutputWriter;
import java.io.Reader;
import org.junit.jupiter.api.Test;

public class ClientProcessingModuleTest {

  private final Injector injector = Guice.createInjector(new ClientProcessingModule());

  @Test
  void configure() {
    // Act / Assert
    injector.getBinding(Reader.class);
    injector.getProvider(OutputWriter.class);
  }
}
