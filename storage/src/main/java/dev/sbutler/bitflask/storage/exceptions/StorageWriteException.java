package dev.sbutler.bitflask.storage.exceptions;

/**
 * Indicates an issue occurred while writing to storage.
 */
public class StorageWriteException extends StorageException {

  public StorageWriteException() {
    super();
  }

  public StorageWriteException(String message) {
    super(message);
  }

  public StorageWriteException(String message, Throwable cause) {
    super(message, cause);
  }

  public StorageWriteException(Throwable cause) {
    super(cause);
  }
}
