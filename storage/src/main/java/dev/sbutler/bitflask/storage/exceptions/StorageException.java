package dev.sbutler.bitflask.storage.exceptions;

/**
 * A generic exception for storage related issues.
 *
 * <p>Prefer creating more specific exceptions as subclasses.
 */
public class StorageException extends RuntimeException {

  public StorageException() {
    super();
  }

  public StorageException(String message) {
    super(message);
  }

  public StorageException(String message, Throwable cause) {
    super(message, cause);
  }

  public StorageException(Throwable cause) {
    super(cause);
  }
}
