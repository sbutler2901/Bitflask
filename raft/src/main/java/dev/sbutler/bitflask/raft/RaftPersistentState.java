package dev.sbutler.bitflask.raft;

import com.google.common.base.Preconditions;
import jakarta.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Persistent state on all servers.
 *
 * <p>Updated on stable storage before responding to RPCs
 */
@Singleton
final class RaftPersistentState {

  /** The RaftServerId for this server. */
  @ThisRaftServerId private final RaftServerId thisRaftServiceId;
  /** The RaftLog for this server. */
  private final RaftLog raftLog;
  /** Latest term server has seen. */
  private final AtomicLong currentTerm = new AtomicLong(0);

  private final ReentrantLock voteLock = new ReentrantLock();

  /** Candidate ID that received vote in current term (or null if none). */
  private volatile RaftServerId votedForCandidateId = null;

  private volatile long termWhenVotedForCandidate = 0L;

  RaftPersistentState(
      @ThisRaftServerId RaftServerId thisRaftServiceId,
      RaftLog raftLog,
      long latestTermSeen,
      RaftServerId votedForCandidateId) {
    this.thisRaftServiceId = thisRaftServiceId;
    this.raftLog = raftLog;
    this.currentTerm.set(latestTermSeen);
    this.votedForCandidateId = votedForCandidateId;
  }

  /** Retrieves the Raft log. */
  RaftLog getRaftLog() {
    return raftLog;
  }

  /** Returns the latest term this server has seen. */
  long getCurrentTerm() {
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
  void setCurrentTermAndResetVote(long newCurrentTerm) {
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
      votedForCandidateId = thisRaftServiceId;
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
