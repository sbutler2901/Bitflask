package dev.sbutler.bitflask.raft;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Service;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import dev.sbutler.bitflask.common.concurrency.VirtualThreadConcurrencyModule;
import dev.sbutler.bitflask.common.guice.RootModule;

/** Module for using the Raft Consensus protocol. */
public class RaftModule extends RootModule {

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

  @Override
  public ImmutableSet<Service> getServices(Injector injector) {
    return ImmutableSet.of(
        injector.getInstance(RaftClusterRpcChannelManager.class),
        injector.getInstance(RaftEntryApplier.class),
        injector.getInstance(RaftServer.class));
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
