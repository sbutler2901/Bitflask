package dev.sbutler.bitflask.client.repl;

import com.google.inject.AbstractModule;
import dev.sbutler.bitflask.client.repl.input.InputParser;
import dev.sbutler.bitflask.client.repl.input.StdinInputParser;
import dev.sbutler.bitflask.client.repl.output.OutputWriter;
import dev.sbutler.bitflask.client.repl.output.StdoutOutputWriter;

public class ReplModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(InputParser.class).to(StdinInputParser.class);
    bind(OutputWriter.class).to(StdoutOutputWriter.class);
  }
}