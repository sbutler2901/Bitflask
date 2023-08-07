package dev.sbutler.bitflask.storage.raft;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/** Manages and provides access to all state necessary for a Raft Server. */
@Singleton
final class RaftStateManager {

  private final RaftPersistentState raftPersistentState;
  private final RaftVolatileState raftVolatileState;
  private final RaftLeaderState raftLeaderState;

  @Inject
  RaftStateManager(
      RaftPersistentState raftPersistentState,
      RaftVolatileState raftVolatileState,
      RaftLeaderState raftLeaderState) {
    this.raftPersistentState = raftPersistentState;
    this.raftVolatileState = raftVolatileState;
    this.raftLeaderState = raftLeaderState;
  }

  RaftPersistentState getRaftPersistentState() {
    return raftPersistentState;
  }

  RaftVolatileState getRaftVolatileState() {
    return raftVolatileState;
  }

  RaftLeaderState getRaftLeaderState() {
    return raftLeaderState;
  }
}
