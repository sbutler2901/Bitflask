package dev.sbutler.bitflask.raft;

import dev.sbutler.bitflask.raft.Entry.DeleteCommand;
import dev.sbutler.bitflask.raft.Entry.SetCommand;

/** The interface for using the Raft Consensus protocol. */
public final class Raft {

  /** A blocking call that returns once a command has successfully committed. */
  public void commitCommand(SetCommand setCommand) {}

  /** A blocking call that returns once a command has successfully committed. */
  public void commitCommand(DeleteCommand deleteCommand) {}

  public boolean isLeader() {
    return false;
  }
  /** Return the current raft leader. */
  public void getLeader() {
    // TODO: establish and return server configuration
  }
}
