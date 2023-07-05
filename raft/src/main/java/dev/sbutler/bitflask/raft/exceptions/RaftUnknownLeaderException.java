package dev.sbutler.bitflask.raft.exceptions;

/** Indicates the current leader of the Raft cluster is unknown. */
public class RaftUnknownLeaderException extends RaftException {

  public RaftUnknownLeaderException(String message) {
    super(message);
  }
}
