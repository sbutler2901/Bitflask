package dev.sbutler.bitflask.client.client_processing;

import com.google.inject.AbstractModule;
import dev.sbutler.bitflask.client.client_processing.output.OutputWriter;
import dev.sbutler.bitflask.client.client_processing.output.StdoutOutputWriter;
import java.io.Reader;
import javax.inject.Singleton;

public class ClientProcessingModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(Reader.class)
        .toProvider(ReaderProvider.class)
        .in(Singleton.class);
    bind(OutputWriter.class)
        .to(StdoutOutputWriter.class);
  }
}
