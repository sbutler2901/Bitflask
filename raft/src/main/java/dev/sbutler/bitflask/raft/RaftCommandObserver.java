package dev.sbutler.bitflask.raft;

/**
 * Implementers are registered and then called whenever a {@link RaftCommand} has been successfully
 * committed.
 */
public interface RaftCommandObserver {

  /**
   * Called when a new {@link dev.sbutler.bitflask.raft.RaftCommand} has been called.
   *
   * <p>This method should complete quickly, longer work should be offloaded to another thread.
   */
  void acceptRaftCommand(RaftCommand raftCommand);
}
