package dev.sbutler.bitflask.raft;

/** Supports committing {@link RaftCommand}s. */
interface CommandSubmitter {

  /** A blocking call that commits the provided {@link RaftCommand} returning true if successful */
  boolean submitCommand(RaftCommand raftCommand);
}
