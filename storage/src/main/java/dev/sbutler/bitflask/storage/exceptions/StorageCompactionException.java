package dev.sbutler.bitflask.storage.exceptions;

/**
 * Indicates an issue occurred while performing storage compaction.
 */
public class StorageCompactionException extends StorageException {

  public StorageCompactionException() {
    super();
  }

  public StorageCompactionException(String message) {
    super(message);
  }

  public StorageCompactionException(String message, Throwable cause) {
    super(message, cause);
  }

  public StorageCompactionException(Throwable cause) {
    super(cause);
  }

}
