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
  private volatile RaftMode raftMode;

  @Inject
  RaftModeManager(RaftElectionTimer raftElectionTimer) {
    this.raftElectionTimer = raftElectionTimer;
    transitionToFollowerState();
  }

  /** Returns the current {@link RaftMode} of the server. */
  RaftMode getCurrentRaftMode() {
    return raftMode;
  }

  /**
   * Returns the current {@link RaftStateProcessor} based on the current {@link RaftMode} of the
   * server.
   */
  RaftStateProcessor getRaftStateProcessor() {
    return raftStateProcessor;
  }

  /** Updates the Raft's server state as a result of an election timer timeout. */
  void handleElectionTimeout() {
    // TODO: implement various transitions.
  }

  /** Transitions the server to the {@link RaftMode#FOLLOWER} state. */
  void transitionToFollowerState() {
    Preconditions.checkState(
        !RaftMode.FOLLOWER.equals(raftMode),
        "The Raft server must be in the CANDIDATE or LEADER state to transition to the FOLLOWER state.");

    stateLock.lock();
    try {
      raftMode = RaftMode.FOLLOWER;
      raftStateProcessor = new RaftFollowerProcessor();
    } finally {
      stateLock.unlock();
    }
  }

  /** Transitions the server to the {@link RaftMode#CANDIDATE} state. */
  void transitionToCandidateState() {
    Preconditions.checkState(
        RaftMode.FOLLOWER.equals(raftMode),
        "The Raft server must be in the FOLLOWER state to transition to the CANDIDATE state.");

    stateLock.lock();
    try {
      raftMode = RaftMode.CANDIDATE;
      raftStateProcessor = new RaftCandidateProcessor();
    } finally {
      stateLock.unlock();
    }
  }

  /** Transitions the server to the {@link RaftMode#LEADER} state. */
  void transitionToLeaderState() {
    Preconditions.checkState(
        RaftMode.CANDIDATE.equals(raftMode),
        "The Raft server must be in the CANDIDATE state to transition to the LEADER state.");

    stateLock.lock();
    try {
      raftMode = RaftMode.LEADER;
      raftStateProcessor = new RaftLeaderProcessor();
    } finally {
      stateLock.unlock();
    }
  }

  /** Represents the modes a Raft server can be in. */
  enum RaftMode {
    /** The server is only listening from incoming RPCs. */
    FOLLOWER,
    /** The server is attempting to become the leader of the cluster. */
    CANDIDATE,
    /** The server is receiving client requests and replicating across the cluster. */
    LEADER
  }
}
