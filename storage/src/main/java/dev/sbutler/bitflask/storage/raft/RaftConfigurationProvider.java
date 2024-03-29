package dev.sbutler.bitflask.storage.raft;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import dev.sbutler.bitflask.config.RaftConfig;
import dev.sbutler.bitflask.config.ServerConfig;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Handles providing a {@link RaftConfiguration} instance using data from {@link ServerConfig} and
 * {@link RaftConfig}.
 */
final class RaftConfigurationProvider implements Provider<RaftConfiguration> {

  private final ServerConfig serverConfig;
  private final RaftConfig raftConfig;
  private final Supplier<RaftConfiguration> raftConfiguration =
      Suppliers.memoize(this::supplyRaftConfiguration);

  @Inject
  RaftConfigurationProvider(ServerConfig serverConfig, RaftConfig raftConfig) {
    this.serverConfig = serverConfig;
    this.raftConfig = raftConfig;
  }

  @Override
  public RaftConfiguration get() {
    return raftConfiguration.get();
  }

  private RaftConfiguration supplyRaftConfiguration() {
    ImmutableMap<RaftServerId, ServerConfig.ServerInfo> clusterServers =
        serverConfig.getBitflaskServersList().stream()
            .collect(
                toImmutableMap(info -> new RaftServerId(info.getServerId()), Function.identity()));
    return new RaftConfiguration(
        new RaftServerId(serverConfig.getThisServerId()),
        clusterServers,
        new RaftTimerInterval(
            raftConfig.getTimerMinimumMilliseconds(), raftConfig.getTimerMaximumMilliseconds()));
  }
}
