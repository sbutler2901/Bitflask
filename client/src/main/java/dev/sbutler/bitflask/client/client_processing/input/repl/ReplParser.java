package dev.sbutler.bitflask.client.client_processing.input.repl;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.client.client_processing.input.repl.types.ReplElement;
import java.io.IOException;

public final class ReplParser {

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
