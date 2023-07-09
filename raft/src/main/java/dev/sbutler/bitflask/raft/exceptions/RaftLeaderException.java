package dev.sbutler.bitflask.raft.exceptions;

/**
 * Exception thrown for errors that occur while the Raft server is handling Leader responsibilities.
 */
public class RaftLeaderException extends RaftException {

  public RaftLeaderException() {
    super();
  }

  public RaftLeaderException(String message) {
    super(message);
  }

  public RaftLeaderException(String message, Throwable cause) {
    super(message, cause);
  }

  public RaftLeaderException(Throwable cause) {
    super(cause);
  }
}
