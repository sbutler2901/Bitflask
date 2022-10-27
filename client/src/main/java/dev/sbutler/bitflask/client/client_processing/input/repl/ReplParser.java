package dev.sbutler.bitflask.client.client_processing.input.repl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import dev.sbutler.bitflask.client.client_processing.input.repl.types.ReplElement;
import java.io.IOException;

public final class ReplParser {

  public static ImmutableList<ReplElement> readNextLine(ReplReader reader) {
    try {
      ImmutableList.Builder<ReplElement> input = new Builder<>();
      input.add(reader.readReplString());
      input.addAll(reader.readToEndLine());
      return input.build();
    } catch (IOException e) {
      throw new ReplIOException("Failed to read next line", e);
    }
  }

  private ReplParser() {
  }
}
