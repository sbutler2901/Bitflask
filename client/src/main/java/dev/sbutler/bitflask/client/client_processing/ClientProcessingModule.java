package dev.sbutler.bitflask.client.client_processing;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.sbutler.bitflask.client.client_processing.output.OutputWriter;
import dev.sbutler.bitflask.client.client_processing.output.StdoutOutputWriter;
import dev.sbutler.bitflask.client.client_processing.repl.ReplReader;
import java.io.Reader;
import javax.inject.Singleton;

public class ClientProcessingModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(Reader.class)
        .toProvider(ReaderProvider.class)
        .in(Singleton.class);
  }

  @Provides
  @Singleton
  ReplReader provideReplReader(Reader reader) {
    return new ReplReader(reader);
  }

  @Provides
  OutputWriter provideOutputWriter(StdoutOutputWriter outputWriter) {
    return outputWriter;
  }
}
