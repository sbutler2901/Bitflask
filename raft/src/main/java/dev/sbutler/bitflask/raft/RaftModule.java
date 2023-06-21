package dev.sbutler.bitflask.raft;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

/** Module for using the Raft Consensus protocol. */
public class RaftModule extends AbstractModule {

  private final RaftServerId raftServerId;
  private final RaftClusterConfiguration raftClusterConfiguration;

  public RaftModule(RaftServerId raftServerId, RaftClusterConfiguration raftClusterConfiguration) {
    this.raftServerId = raftServerId;
    this.raftClusterConfiguration = raftClusterConfiguration;
  }

  @Provides
  RaftServerId provideRaftServerId() {
    return this.raftServerId;
  }

  @Provides
  RaftServerInfo provideRaftServerInfo() {
    return raftClusterConfiguration.clusterServers().get(raftServerId);
  }

  @Provides
  RaftClusterConfiguration provideRaftClusterConfiguration() {
    return raftClusterConfiguration;
  }

  @Provides
  RaftServer provideRaftServer(RaftServer raftServer) {
    return raftServer;
  }
}
