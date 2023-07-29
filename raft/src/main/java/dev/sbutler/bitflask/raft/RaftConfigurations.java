package dev.sbutler.bitflask.raft;

import static com.google.mu.util.stream.GuavaCollectors.toImmutableMap;

import com.google.common.collect.ImmutableMap;
import com.google.mu.util.stream.BiStream;

/** The configuration for a Raft cluster. */
public record RaftConfigurations(
    RaftServerId thisRaftServerId,
    ImmutableMap<RaftServerId, RaftServerInfo> clusterServers,
    RaftTimerInterval raftTimerInterval) {

  RaftServerInfo thisRaftServerInfo() {
    return clusterServers.get(thisRaftServerId);
  }

  /** Gets the other servers in the Raft cluster besides this instance. */
  ImmutableMap<RaftServerId, RaftServerInfo> getOtherServersInCluster() {
    return BiStream.from(clusterServers)
        .filterKeys(raftServerId -> raftServerId != thisRaftServerId)
        .collect(toImmutableMap());
  }
}
