package dev.sbutler.bitflask.storage.raft.exceptions;

/** Indicates the current leader of the Raft cluster is unknown. */
public class RaftUnknownLeaderException extends RaftModeException {

  public RaftUnknownLeaderException(String message) {
    super(message);
  }
}
