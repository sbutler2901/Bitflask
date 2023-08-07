package dev.sbutler.bitflask.storage.raft;

/** Methods for registering and unregistering {@link RaftCommandObserver}s. */
public interface RaftCommandSubjectRegistrar {
  void register(RaftCommandObserver observer);

  void unregister(RaftCommandObserver observer);
}
