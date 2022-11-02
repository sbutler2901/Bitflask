package dev.sbutler.bitflask.client.client_processing.repl;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.client.client_processing.repl.types.ReplElement;
import java.util.Optional;

/**
 * Used to interface with a {@link ReplReader} for input parsing needs.
 */
public final class ReplParser {

  /**
   * Reads the next line of input and parses into ReplElements.
   * <p>
   * The returned Optional will be empty when there is no longer input data to be parsed. of input
   * has been reached. An empty list indicates there was no input to be parsed.
   */
  public static Optional<ImmutableList<ReplElement>> readNextLine(ReplReader reader)
      throws ReplSyntaxException, ReplIOException {
    return reader.readToEndLine();
  }

  /**
   * Used to consume all input data until the end of the line or document has been reached.
   *
   * <p>No parsing will be performed and input data related errors will be ignored.
   */
  public static void cleanupForNextLine(ReplReader replReader) throws ReplIOException {
    replReader.readToEndLineWithoutParsing();
  }

  private ReplParser() {
  }
}
