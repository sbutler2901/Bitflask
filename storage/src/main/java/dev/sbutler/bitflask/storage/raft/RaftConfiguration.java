package dev.sbutler.bitflask.storage.raft;

import static com.google.mu.util.stream.GuavaCollectors.toImmutableMap;

import com.google.common.collect.ImmutableMap;
import com.google.mu.util.stream.BiStream;
import dev.sbutler.bitflask.config.ServerConfig;

/** The configuration for a Raft cluster. */
public record RaftConfiguration(
    RaftServerId thisRaftServerId,
    ImmutableMap<RaftServerId, ServerConfig.ServerInfo> clusterServers,
    RaftTimerInterval raftTimerInterval) {

  /** Gets the other servers in the Raft cluster besides this instance. */
  ImmutableMap<RaftServerId, ServerConfig.ServerInfo> getOtherServersInCluster() {
    return BiStream.from(clusterServers)
        .filterKeys(raftServerId -> !raftServerId.equals(thisRaftServerId))
        .collect(toImmutableMap());
  }

  ServerConfig.ServerInfo getThisServerInfo() {
    return clusterServers.get(thisRaftServerId);
  }
}
