package dev.sbutler.bitflask.client.client_processing;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.sbutler.bitflask.client.client_processing.output.OutputWriter;
import dev.sbutler.bitflask.client.client_processing.output.StdoutOutputWriter;

public class ClientProcessingModule extends AbstractModule {

  @Provides
  OutputWriter provideOutputWriter(StdoutOutputWriter outputWriter) {
    return outputWriter;
  }
}