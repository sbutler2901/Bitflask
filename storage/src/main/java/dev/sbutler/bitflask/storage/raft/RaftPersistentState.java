package dev.sbutler.bitflask.storage.raft;

import com.google.common.base.Preconditions;
import jakarta.inject.Inject;
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

  /** Latest term server has seen. */
  private final AtomicInteger currentTerm = new AtomicInteger(0);

  /** Candidate ID that received vote in current term (or null if none). */
  private volatile RaftServerId votedForCandidateId;

  // Helper fields
  private final RaftConfiguration raftConfiguration;
  private final ReentrantLock voteLock = new ReentrantLock();
  private volatile int termWhenVotedForCandidate = 0;

  @Inject
  RaftPersistentState(RaftConfiguration raftConfiguration) {
    this.raftConfiguration = raftConfiguration;
  }

  /** Used to initialize state at startup. */
  void initialize(int latestTermSeen, Optional<RaftServerId> votedForCandidateId) {
    voteLock.lock();
    try {
      currentTerm.set(latestTermSeen);
      this.votedForCandidateId = votedForCandidateId.orElse(null);
    } finally {
      voteLock.unlock();
    }
  }

  /** Returns the latest term this server has seen. */
  int getCurrentTerm() {
    return currentTerm.get();
  }

  /** Sets the current term and resets this server's vote. */
  void setCurrentTermAndResetVote(int newCurrentTerm) {
    Preconditions.checkArgument(newCurrentTerm >= currentTerm.get());
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
      votedForCandidateId = raftConfiguration.thisRaftServerId();
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
