package dev.sbutler.bitflask.raft;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Service;
import com.google.inject.Injector;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import dev.sbutler.bitflask.common.guice.RootModule;

/** Module for using the Raft Consensus protocol. */
public class RaftModule extends RootModule {

  @Override
  protected void configure() {
    install(new FactoryModuleBuilder().build(RaftModeProcessor.Factory.class));
    bind(RaftConfiguration.class).toProvider(RaftConfigurationProvider.class);
  }

  @Override
  public ImmutableSet<Service> getServices(Injector injector) {
    return ImmutableSet.of(
        injector.getInstance(RaftClusterRpcChannelManager.class),
        injector.getInstance(RaftEntryApplier.class),
        injector.getInstance(RaftServer.class));
  }
}
