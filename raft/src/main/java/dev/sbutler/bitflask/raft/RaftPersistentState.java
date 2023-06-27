package dev.sbutler.bitflask.raft;

import com.google.common.base.Preconditions;
import jakarta.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Persistent state on all servers.
 *
 * <p>Updated on stable storage before responding to RPCs
 */
@Singleton
final class RaftPersistentState {

  // Persisted fields
  /** The RaftLog for this server. */
  private final RaftLog raftLog;
  /** Latest term server has seen. */
  private final AtomicInteger currentTerm = new AtomicInteger(0);
  /** Candidate ID that received vote in current term (or null if none). */
  private volatile RaftServerId votedForCandidateId;

  // Helper fields
  private final RaftClusterConfiguration raftClusterConfiguration;
  private final ReentrantLock voteLock = new ReentrantLock();
  private volatile int termWhenVotedForCandidate = 0;

  RaftPersistentState(
      RaftClusterConfiguration raftClusterConfiguration,
      RaftLog raftLog,
      int latestTermSeen,
      RaftServerId votedForCandidateId) {
    this.raftClusterConfiguration = raftClusterConfiguration;
    this.raftLog = raftLog;
    this.currentTerm.set(latestTermSeen);
    this.votedForCandidateId = votedForCandidateId;
  }

  /** Retrieves the Raft log. */
  RaftLog getRaftLog() {
    return raftLog;
  }

  /** Returns the latest term this server has seen. */
  int getCurrentTerm() {
    return currentTerm.get();
  }

  /** Increments the current term returning the previous value. */
  void incrementCurrentTermAndResetVote() {
    voteLock.lock();
    try {
      currentTerm.getAndIncrement();
      votedForCandidateId = null;
    } finally {
      voteLock.unlock();
    }
  }

  /** Sets the current term and resets this server's vote. */
  void setCurrentTermAndResetVote(int newCurrentTerm) {
    voteLock.lock();
    try {
      currentTerm.set(newCurrentTerm);
      votedForCandidateId = null;
    } finally {
      voteLock.unlock();
    }
  }

  /** Gets the candidate ID that the server has voted for in the current term, if it has voted. */
  Optional<RaftServerId> getVotedForCandidateId() {
    voteLock.lock();
    try {
      return Optional.ofNullable(votedForCandidateId);
    } finally {
      voteLock.unlock();
    }
  }

  /** Votes for this server. */
  void incrementTermAndVoteForSelf() {
    voteLock.lock();
    try {
      currentTerm.getAndIncrement();
      votedForCandidateId = raftClusterConfiguration.thisRaftServerId();
      termWhenVotedForCandidate = currentTerm.get();
    } finally {
      voteLock.unlock();
    }
  }

  /** Sets the candidate ID that the server has voted for in the current term. */
  void setVotedForCandidateId(RaftServerId candidateId) {
    Preconditions.checkState(
        currentTerm.get() > termWhenVotedForCandidate,
        "This Raft server has already voted for a candidate this term.");

    voteLock.lock();
    try {
      votedForCandidateId = candidateId;
      termWhenVotedForCandidate = currentTerm.get();
    } finally {
      voteLock.unlock();
    }
  }
}
