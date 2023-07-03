package dev.sbutler.bitflask.raft.exceptions;

/**
 * A generic exception for Raft related issues.
 *
 * <p>Prefer creating more specific exceptions as subclasses.
 */
public class RaftException extends RuntimeException {
  public RaftException() {
    super();
  }

  public RaftException(String message) {
    super(message);
  }

  public RaftException(String message, Throwable cause) {
    super(message, cause);
  }

  public RaftException(Throwable cause) {
    super(cause);
  }
}
