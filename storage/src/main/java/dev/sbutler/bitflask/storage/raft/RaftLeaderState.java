package dev.sbutler.bitflask.storage.raft;

import static dev.sbutler.bitflask.storage.raft.RaftLeaderProcessor.AppendEntriesSubmission;

import jakarta.inject.Inject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Volatile state of a server that is the Raft leader.
 *
 * <p>This must be reset after each election.
 */
final class RaftLeaderState {

  /**
   * For each server, index of the next log entry to send to that server (initialized to leader last
   * log index + 1)
   */
  private final ConcurrentMap<RaftServerId, AtomicInteger> followersNextIndex =
      new ConcurrentHashMap<>();

  /**
   * For each server, index of highest log entry known to be replicated on server (initialized to 0,
   * increases monotonically)
   */
  private final ConcurrentMap<RaftServerId, AtomicInteger> followersMatchIndex =
      new ConcurrentHashMap<>();

  @Inject
  RaftLeaderState(RaftConfiguration raftConfiguration, RaftLog raftLog) {
    int nextIndex = raftLog.getLastLogEntryDetails().index() + 1;
    for (var followerServerId : raftConfiguration.getOtherServersInCluster().keySet()) {
      followersNextIndex.put(followerServerId, new AtomicInteger(nextIndex));
      followersMatchIndex.put(followerServerId, new AtomicInteger(0));
    }
  }

  int getFollowerNextIndex(RaftServerId serverId) {
    return followersNextIndex.get(serverId).get();
  }

  int getFollowerMatchIndex(RaftServerId serverId) {
    return followersMatchIndex.get(serverId).get();
  }

  /**
   * Reduces the provided server's next index based on the provided {@link AppendEntriesSubmission}.
   */
  void decreaseFollowerNextIndex(AppendEntriesSubmission submission) {
    followersNextIndex
        .get(submission.serverId())
        .getAndUpdate(prev -> Math.min(prev, submission.followerNextIndex() - 1));
  }

  void increaseFollowerNextIndex(AppendEntriesSubmission submission) {
    followersNextIndex
        .get(submission.serverId())
        .getAndUpdate(prev -> Math.max(prev, submission.lastEntryIndex()));
  }

  void increaseFollowerMatchIndex(AppendEntriesSubmission submission) {
    followersMatchIndex
        .get(submission.serverId())
        .getAndUpdate(prev -> Math.max(prev, submission.lastEntryIndex()));
  }
}
