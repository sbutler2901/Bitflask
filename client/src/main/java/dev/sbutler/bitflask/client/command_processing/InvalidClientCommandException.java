package dev.sbutler.bitflask.client.command_processing;

/** Used to indicate a client provided command was invalid. */
public class InvalidClientCommandException extends ProcessingException {

  public InvalidClientCommandException(String message) {
    super(message);
  }
}
