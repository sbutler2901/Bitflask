package dev.sbutler.bitflask.raft;

/**
 * Methods for registering and unregistering {@link dev.sbutler.bitflask.raft.RaftCommandObserver}s.
 */
public interface RaftCommandSubjectRegistrar {
  void register(RaftCommandObserver obj);

  void unregister(RaftCommandObserver obj);
}
