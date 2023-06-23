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

  private volatile RaftModeProcessor raftModeProcessor;

  @Inject
  RaftModeManager(RaftElectionTimer raftElectionTimer) {
    this.raftElectionTimer = raftElectionTimer;
    transitionToFollowerState();
  }

  /** Returns the current {@link RaftMode} of the server. */
  RaftMode getCurrentRaftMode() {
    return switch (raftModeProcessor) {
      case RaftFollowerProcessor _unused -> RaftMode.FOLLOWER;
      case RaftCandidateProcessor _unused -> RaftMode.CANDIDATE;
      case RaftLeaderProcessor _unused -> RaftMode.LEADER;
    };
  }

  /**
   * Returns the current {@link RaftModeProcessor} based on the current {@link RaftMode} of the
   * server.
   */
  RaftModeProcessor getRaftStateProcessor() {
    return raftModeProcessor;
  }

  /** Transitions the server to the {@link RaftMode#FOLLOWER} state. */
  void transitionToFollowerState() {
    Preconditions.checkState(
        !RaftMode.FOLLOWER.equals(getCurrentRaftMode()),
        "The Raft server must be in the CANDIDATE or LEADER state to transition to the FOLLOWER state.");

    stateLock.lock();
    try {
      raftModeProcessor = new RaftFollowerProcessor();
    } finally {
      stateLock.unlock();
    }
  }

  /** Transitions the server to the {@link RaftMode#CANDIDATE} state. */
  void transitionToCandidateState() {
    Preconditions.checkState(
        RaftMode.FOLLOWER.equals(getCurrentRaftMode()),
        "The Raft server must be in the FOLLOWER state to transition to the CANDIDATE state.");

    stateLock.lock();
    try {
      raftModeProcessor = new RaftCandidateProcessor();
    } finally {
      stateLock.unlock();
    }
  }

  /** Transitions the server to the {@link RaftMode#LEADER} state. */
  void transitionToLeaderState() {
    Preconditions.checkState(
        RaftMode.CANDIDATE.equals(getCurrentRaftMode()),
        "The Raft server must be in the CANDIDATE state to transition to the LEADER state.");

    stateLock.lock();
    try {
      raftModeProcessor = new RaftLeaderProcessor();
    } finally {
      stateLock.unlock();
    }
  }

  /** Updates the Raft's server state as a result of an election timer timeout. */
  void handleElectionTimeout() {
    stateLock.lock();
    try {
      switch (getCurrentRaftMode()) {
        case FOLLOWER -> transitionToCandidateState();
        case CANDIDATE -> transitionToLeaderState();
        case LEADER -> throw new IllegalStateException(
            "Raft in LEADER mode should not have an election timer running");
      }
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
