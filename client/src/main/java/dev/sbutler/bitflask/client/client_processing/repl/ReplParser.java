package dev.sbutler.bitflask.client.client_processing.repl;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.client.client_processing.repl.types.ReplElement;
import java.io.IOException;

public final class ReplParser {

  /**
   * Reads the next line of input and parses into ReplElements. Null will be returned when the end
   * of input has been reached. An empty list indicates there was no input to be parsed.
   */
  public static ImmutableList<ReplElement> readNextLine(ReplReader reader)
      throws ReplSyntaxException, ReplIOException {
    try {
      return reader.readToEndLine();
    } catch (IOException e) {
      throw new ReplIOException("Failed to read next line", e);
    }
  }

  private ReplParser() {
  }
}
