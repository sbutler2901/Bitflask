package dev.sbutler.bitflask.raft;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/** The interface for using the Raft Consensus protocol. */
@Singleton
public final class Raft implements CommandCommitter {

  private final RaftModeManager raftModeManager;

  @Inject
  Raft(RaftModeManager raftModeManager) {
    this.raftModeManager = raftModeManager;
  }

  public boolean commitCommand(SetCommand setCommand) {
    return true;
  }

  public boolean commitCommand(DeleteCommand deleteCommand) {
    return true;
  }

  public boolean isLeader() {
    return false;
  }
  /** Return the current raft leader. */
  public void getLeader() {
    // TODO: establish and return server configuration
  }
}
