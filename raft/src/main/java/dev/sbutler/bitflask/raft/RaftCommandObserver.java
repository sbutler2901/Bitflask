package dev.sbutler.bitflask.raft;

/**
 * Implementers are registered and then called whenever a {@link RaftCommand} has been successfully
 * committed.
 */
public interface RaftCommandObserver {

  void acceptRaftCommand(RaftCommand raftCommand);
}
