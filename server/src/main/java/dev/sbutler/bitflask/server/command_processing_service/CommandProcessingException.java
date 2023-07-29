package dev.sbutler.bitflask.server.command_processing_service;

/**
 * A generic exception for issues that occur while processing a client's command.
 *
 * <p>Prefer creating more specific exceptions as subclasses.
 */
public class CommandProcessingException extends RuntimeException {
  CommandProcessingException() {
    super();
  }

  CommandProcessingException(String message) {
    super(message);
  }

  CommandProcessingException(String message, Throwable cause) {
    super(message, cause);
  }

  CommandProcessingException(Throwable cause) {
    super(cause);
  }
}
