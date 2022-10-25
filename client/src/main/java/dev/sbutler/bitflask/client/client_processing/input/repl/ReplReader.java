package dev.sbutler.bitflask.client.client_processing.input.repl;

import dev.sbutler.bitflask.client.client_processing.input.repl.types.ReplElement;
import java.io.Reader;

public final class ReplReader implements AutoCloseable {

  private final Reader reader;

  public ReplReader(Reader reader) {
    this.reader = reader;
  }

  public ReplElement getNextElement() {
    // TODO: implement
    return null;
  }

  @Override
  public void close() throws Exception {

  }
}
