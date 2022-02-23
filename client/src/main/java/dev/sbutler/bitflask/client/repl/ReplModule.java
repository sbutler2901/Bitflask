package dev.sbutler.bitflask.client.repl;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.sbutler.bitflask.client.repl.input.InputParser;
import dev.sbutler.bitflask.client.repl.input.StdinInputParser;
import dev.sbutler.bitflask.client.repl.output.OutputWriter;
import dev.sbutler.bitflask.client.repl.output.StdoutOutputWriter;

public class ReplModule extends AbstractModule {

  @Provides
  InputParser provideInputParser(StdinInputParser inputParser) {
    return inputParser;
  }

  @Provides
  OutputWriter provideOutputWriter(StdoutOutputWriter outputWriter) {
    return outputWriter;
  }
}