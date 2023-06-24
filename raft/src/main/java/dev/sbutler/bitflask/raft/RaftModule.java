package dev.sbutler.bitflask.raft;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import dev.sbutler.bitflask.common.concurrency.VirtualThreadConcurrencyModule;

/** Module for using the Raft Consensus protocol. */
public class RaftModule extends AbstractModule {

  private final RaftClusterConfiguration raftClusterConfiguration;
  private final boolean standaloneMode;

  private RaftModule(RaftClusterConfiguration raftClusterConfiguration, boolean standaloneMode) {
    this.raftClusterConfiguration = raftClusterConfiguration;
    this.standaloneMode = standaloneMode;
  }

  /** Creates a Raft module for usage within another application. */
  public RaftModule create(RaftClusterConfiguration raftClusterConfiguration) {
    return new RaftModule(raftClusterConfiguration, false);
  }

  /** Creates a Raft module for operating in standalone mode. */
  public RaftModule createStandalone(RaftClusterConfiguration raftClusterConfiguration) {
    return new RaftModule(raftClusterConfiguration, true);
  }

  @Override
  protected void configure() {
    install(new FactoryModuleBuilder().build(RaftModeProcessor.Factory.class));
    if (standaloneMode) {
      install(new VirtualThreadConcurrencyModule());
    }
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
