package dev.sbutler.bitflask.storage.raft.exceptions;

/**
 * A generic exception for Raft mode related issues.
 *
 * <p>Prefer creating more specific exceptions as subclasses.
 */
public class RaftModeException extends RaftException {
  public RaftModeException() {
    super();
  }

  public RaftModeException(String message) {
    super(message);
  }

  public RaftModeException(String message, Throwable cause) {
    super(message, cause);
  }

  public RaftModeException(Throwable cause) {
    super(cause);
  }
}
