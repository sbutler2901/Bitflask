package dev.sbutler.bitflask.raft;

import com.google.common.base.Preconditions;

/** Manages and provides access to all state necessary for a Raft Server. */
final class RaftStateManager {

  private final RaftPersistentState raftPersistentState;
  private final RaftVolatileState raftVolatileState;
  private final RaftLeaderState raftLeaderState;

  private volatile RaftServerState raftServerState;

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

  /** Returns the current {@link RaftServerState} of the server. */
  RaftServerState getCurrentRaftServerState() {
    return raftServerState;
  }

  /** Transitions the server to the {@link RaftServerState#FOLLOWER} state. */
  void transitionToFollowerState() {
    raftServerState = RaftServerState.FOLLOWER;
  }

  /** Transitions the server to the {@link RaftServerState#CANDIDATE} state. */
  void transitionToCandidateState() {
    Preconditions.checkState(
        RaftServerState.FOLLOWER.equals(raftServerState),
        "The Raft server must be in the FOLLOWER state to transition to the CANDIDATE state.");
    raftServerState = RaftServerState.CANDIDATE;
  }

  /** Transitions the server to the {@link RaftServerState#LEADER} state. */
  void transitionToLeaderState() {
    Preconditions.checkState(
        RaftServerState.CANDIDATE.equals(raftServerState),
        "The Raft server must be in the CANDIDATE state to transition to the LEADER state.");
    raftServerState = RaftServerState.LEADER;
  }
}
