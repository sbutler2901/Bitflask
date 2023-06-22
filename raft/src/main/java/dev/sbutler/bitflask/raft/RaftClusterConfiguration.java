package dev.sbutler.bitflask.raft;

import com.google.common.collect.ImmutableMap;

/** The configuration for a Raft cluster. */
public record RaftClusterConfiguration(
    ImmutableMap<RaftServerId, RaftServerInfo> clusterServers,
    RaftTimerInterval raftTimerInterval) {}
