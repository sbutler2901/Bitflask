package dev.sbutler.bitflask.raft;

import com.google.common.base.Preconditions;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.locks.ReentrantLock;

/** Handles Raft's server mode and transitioning between them. */
@Singleton
final class RaftModeManager {

  private final RaftElectionTimer raftElectionTimer;

  private final ReentrantLock stateLock = new ReentrantLock();

  private volatile RaftStateProcessor raftStateProcessor;
  private volatile RaftServerState raftServerState;

  @Inject
  RaftModeManager(RaftElectionTimer raftElectionTimer) {
    this.raftElectionTimer = raftElectionTimer;
    transitionToFollowerState();
  }

  /** Returns the current {@link RaftServerState} of the server. */
  RaftServerState getCurrentRaftServerState() {
    return raftServerState;
  }

  /**
   * Returns the current {@link RaftStateProcessor} based on the current {@link RaftServerState} of
   * the server.
   */
  RaftStateProcessor getRaftStateProcessor() {
    return raftStateProcessor;
  }

  /** Updates the Raft's server state as a result of an election timer timeout. */
  void handleElectionTimeout() {
    // TODO: implement various transitions.
  }

  /** Transitions the server to the {@link RaftServerState#FOLLOWER} state. */
  void transitionToFollowerState() {
    Preconditions.checkState(
        !RaftServerState.FOLLOWER.equals(raftServerState),
        "The Raft server must be in the CANDIDATE or LEADER state to transition to the FOLLOWER state.");

    stateLock.lock();
    try {
      raftServerState = RaftServerState.FOLLOWER;
      raftStateProcessor = new RaftFollowerProcessor();
    } finally {
      stateLock.unlock();
    }
  }

  /** Transitions the server to the {@link RaftServerState#CANDIDATE} state. */
  void transitionToCandidateState() {
    Preconditions.checkState(
        RaftServerState.FOLLOWER.equals(raftServerState),
        "The Raft server must be in the FOLLOWER state to transition to the CANDIDATE state.");

    stateLock.lock();
    try {
      raftServerState = RaftServerState.CANDIDATE;
      raftStateProcessor = new RaftCandidateProcessor();
    } finally {
      stateLock.unlock();
    }
  }

  /** Transitions the server to the {@link RaftServerState#LEADER} state. */
  void transitionToLeaderState() {
    Preconditions.checkState(
        RaftServerState.CANDIDATE.equals(raftServerState),
        "The Raft server must be in the CANDIDATE state to transition to the LEADER state.");

    stateLock.lock();
    try {
      raftServerState = RaftServerState.LEADER;
      raftStateProcessor = new RaftLeaderProcessor();
    } finally {
      stateLock.unlock();
    }
  }
}
