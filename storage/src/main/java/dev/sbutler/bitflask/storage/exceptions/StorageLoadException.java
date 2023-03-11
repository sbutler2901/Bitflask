package dev.sbutler.bitflask.storage.exceptions;

/**
 * Indicates an issue occurred while loading the storage engine at startup.
 */
public class StorageLoadException extends StorageException {

  public StorageLoadException() {
    super();
  }

  public StorageLoadException(String message) {
    super(message);
  }

  public StorageLoadException(String message, Throwable cause) {
    super(message, cause);
  }

  public StorageLoadException(Throwable cause) {
    super(cause);
  }
}
