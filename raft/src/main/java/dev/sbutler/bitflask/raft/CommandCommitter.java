package dev.sbutler.bitflask.raft;

/** Supports committing {@link RaftCommand}s. */
interface CommandCommitter {

  /** A blocking call that commits the provided {@link RaftCommand} returning true if successful */
  boolean commitCommand(RaftCommand raftCommand);
}
