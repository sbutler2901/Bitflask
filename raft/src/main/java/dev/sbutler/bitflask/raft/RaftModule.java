package dev.sbutler.bitflask.raft;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

/** Module for using the Raft Consensus protocol. */
public class RaftModule extends AbstractModule {

  private final RaftServerInfo raftServerInfo;
  private final RaftClusterConfiguration raftClusterConfiguration;

  public RaftModule(
      RaftServerInfo raftServerInfo, RaftClusterConfiguration raftClusterConfiguration) {
    this.raftServerInfo = raftServerInfo;
    this.raftClusterConfiguration = raftClusterConfiguration;
  }

  @Provides
  RaftServerInfo provideRaftServerInfo() {
    return raftServerInfo;
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
