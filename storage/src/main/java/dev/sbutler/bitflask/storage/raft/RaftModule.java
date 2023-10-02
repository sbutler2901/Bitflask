package dev.sbutler.bitflask.storage.raft;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import dev.sbutler.bitflask.common.concurrency.VirtualThreadFactory;
import dev.sbutler.bitflask.common.guice.RootModule;
import jakarta.inject.Singleton;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

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
        injector.getInstance(RaftService.class),
        injector.getInstance(RaftModeManager.class),
        injector.getInstance(RaftClusterRpcChannelManager.class),
        injector.getInstance(RaftEntryApplier.class));
  }

  @Provides
  @Singleton
  @RaftModeManagerListeningExecutorService
  public ListeningExecutorService provideModeManagerExecutorService() {
    ThreadFactory threadFactory = new VirtualThreadFactory("raft-mode-manager-vir-");
    return MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor(threadFactory));
  }

  @Provides
  @Singleton
  @RaftLeaderListeningScheduledExecutorService
  ScheduledExecutorService providedScheduledExecutorService() {
    ThreadFactory threadFactory = new VirtualThreadFactory("raft-leader-vir-");
    return MoreExecutors.listeningDecorator(
        Executors.newSingleThreadScheduledExecutor(threadFactory));
  }
}
