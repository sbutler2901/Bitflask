package dev.sbutler.bitflask.raft;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;

/** Module for using the Raft Consensus protocol. */
public class RaftModule extends AbstractModule {

  private final RaftServerId raftServerId;
  private final RaftClusterConfiguration raftClusterConfiguration;

  public RaftModule(RaftServerId raftServerId, RaftClusterConfiguration raftClusterConfiguration) {
    this.raftServerId = raftServerId;
    this.raftClusterConfiguration = raftClusterConfiguration;
  }

  @Override
  protected void configure() {
    install(new FactoryModuleBuilder().build(RaftModeProcessorFactory.class));
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
