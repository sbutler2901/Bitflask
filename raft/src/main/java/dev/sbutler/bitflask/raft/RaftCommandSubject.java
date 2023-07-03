package dev.sbutler.bitflask.raft;

/**
 * Handles maintaining {@link dev.sbutler.bitflask.raft.RaftCommandObserver}s and notifying them.
 */
interface RaftCommandSubject extends RaftCommandSubjectRegistrar {

  void notifyObservers(RaftCommand raftCommand);
}
