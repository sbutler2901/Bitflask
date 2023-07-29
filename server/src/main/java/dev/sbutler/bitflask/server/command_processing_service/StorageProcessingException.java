package dev.sbutler.bitflask.server.command_processing_service;

/** Used to indicate a major Storage processing error. */
public final class StorageProcessingException extends CommandProcessingException {

  StorageProcessingException(String message) {
    super(message);
  }
}
