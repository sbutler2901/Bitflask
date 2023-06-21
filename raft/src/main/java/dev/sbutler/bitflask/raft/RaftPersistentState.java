package dev.sbutler.bitflask.raft;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Persistent state on all servers.
 *
 * <p>Updated on stable storage before responding to RPCs
 */
final class RaftPersistentState {

  private final RaftLog raftLog;
  /** Latest term server has seen. */
  private final AtomicLong latestTermSeen = new AtomicLong(0);
  /** Candidate ID that received vote in current term (or null if none). */
  private volatile RaftServerId votedForCandidateId = null;

  RaftPersistentState(RaftLog raftLog, long latestTermSeen, RaftServerId votedForCandidateId) {
    this.raftLog = raftLog;
    this.latestTermSeen.set(latestTermSeen);
    this.votedForCandidateId = votedForCandidateId;
  }

  /** Retrieves the Raft log. */
  RaftLog getRaftLog() {
    return raftLog;
  }

  /** Returns the latest term this server has seen. */
  long getLatestTermSeen() {
    return latestTermSeen.get();
  }

  /** Sets the latest term seen by this server to the new value and returns the previous. */
  long setLatestTermSeen(long newCurrentTerm) {
    return latestTermSeen.getAndSet(newCurrentTerm);
  }

  /** Gets the candidate ID that the server has voted for in the current term, if it has voted. */
  Optional<RaftServerId> getVotedForCandidateId() {
    return Optional.ofNullable(votedForCandidateId);
  }

  /** Sets the candidate ID that the server has voted for in the current term. */
  void setVotedForCandidateId(RaftServerId candidateId) {
    votedForCandidateId = candidateId;
  }

  /** Resets the candidate ID that the server has voted. */
  void resetVotedForCandidateId() {
    votedForCandidateId = null;
  }
}
