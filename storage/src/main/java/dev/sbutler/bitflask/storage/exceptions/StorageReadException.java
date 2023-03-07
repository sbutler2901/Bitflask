package dev.sbutler.bitflask.storage.exceptions;

/**
 * Indicates an issue occurred while reading from storage.
 */
public class StorageReadException extends StorageException {

  public StorageReadException() {
    super();
  }

  public StorageReadException(String message) {
    super(message);
  }

  public StorageReadException(String message, Throwable cause) {
    super(message, cause);
  }

  public StorageReadException(Throwable cause) {
    super(cause);
  }
}
