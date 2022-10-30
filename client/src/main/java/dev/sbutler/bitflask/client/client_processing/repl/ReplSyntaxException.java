package dev.sbutler.bitflask.client.client_processing.repl;

/**
 * Used to indicate Repl syntax related issues with the input data.
 */
public class ReplSyntaxException extends Exception {

  public ReplSyntaxException(String message) {
    super(message);
  }

}
