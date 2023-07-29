package dev.sbutler.bitflask.server.command_processing_service;

/** Used to indicate a client's command message was invalid. */
public final class InvalidCommandException extends CommandProcessingException {

  public InvalidCommandException(String message) {
    super(message);
  }
}
