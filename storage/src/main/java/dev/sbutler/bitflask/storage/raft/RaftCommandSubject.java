package dev.sbutler.bitflask.storage.raft;

/** Handles maintaining {@link RaftCommandObserver}s and notifying them. */
interface RaftCommandSubject extends RaftCommandSubjectRegistrar {

  void notifyObservers(RaftCommand raftCommand);
}
