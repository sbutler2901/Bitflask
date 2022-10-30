package dev.sbutler.bitflask.client.client_processing.repl;

/**
 * Used to indicate unrecoverable errors that occur while parsing Repl data.
 */
public class ReplParseException extends RuntimeException {

  public ReplParseException(String message) {
    super(message);
  }
}
