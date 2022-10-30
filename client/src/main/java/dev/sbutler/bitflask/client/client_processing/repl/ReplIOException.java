package dev.sbutler.bitflask.client.client_processing.repl;

import java.io.IOException;

/**
 * Used to indicate IO related errors while parsing Repl data.
 */
public class ReplIOException extends IOException {

  public ReplIOException(String message, Throwable cause) {
    super(message, cause);
  }
}
