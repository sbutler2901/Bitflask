package dev.sbutler.bitflask.raft;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;

/** Module for using the Raft Consensus protocol. */
public class RaftModule extends AbstractModule {

  private final RaftClusterConfiguration raftClusterConfiguration;

  public RaftModule(RaftClusterConfiguration raftClusterConfiguration) {
    this.raftClusterConfiguration = raftClusterConfiguration;
  }

  @Override
  protected void configure() {
    install(new FactoryModuleBuilder().build(RaftModeProcessor.Factory.class));
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
