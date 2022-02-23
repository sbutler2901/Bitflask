package dev.sbutler.bitflask.client.client_processing;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.sbutler.bitflask.client.client_processing.input.InputParser;
import dev.sbutler.bitflask.client.client_processing.input.StdinInputParser;
import dev.sbutler.bitflask.client.client_processing.output.OutputWriter;
import dev.sbutler.bitflask.client.client_processing.output.StdoutOutputWriter;
import dev.sbutler.bitflask.client.client_processing.repl.Repl;

public class ClientProcessingModule extends AbstractModule {

  @Provides
  ClientProcessor provideClientProcessor(Repl repl) {
    return repl;
  }

  @Provides
  InputParser provideInputParser(StdinInputParser inputParser) {
    return inputParser;
  }

  @Provides
  OutputWriter provideOutputWriter(StdoutOutputWriter outputWriter) {
    return outputWriter;
  }
}
