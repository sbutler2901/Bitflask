package dev.sbutler.bitflask.raft;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
  private final ConcurrentMap<RaftServerId, Long> nextIndex = new ConcurrentHashMap<>();

  /**
   * For each server, index of highest log entry known to be replicated on server (initialized to 0,
   * increases monotonically)
   */
  private final ConcurrentMap<RaftServerId, Long> matchIndex = new ConcurrentHashMap<>();

  RaftLeaderState(RaftClusterConfiguration clusterConfiguration) {
    initialize(clusterConfiguration.clusterServers().keySet());
  }

  /**
   * Initialized the state.
   *
   * <p>Must be used after an election.
   */
  void reinitialized() {
    initialize(nextIndex.keySet());
  }

  private void initialize(Iterable<RaftServerId> serverIds) {
    for (RaftServerId serverId : serverIds) {
      nextIndex.put(serverId, 0L);
      matchIndex.put(serverId, 0L);
    }
  }
}
