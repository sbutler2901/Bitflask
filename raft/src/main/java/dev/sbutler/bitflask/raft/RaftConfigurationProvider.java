package dev.sbutler.bitflask.raft;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.google.common.base.Converter;
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
    ImmutableMap<RaftServerId, RaftServerInfo> clusterServers =
        serverConfig.getBitflaskServersList().stream()
            .map(ServerInfoConverter.INSTANCE::doForward)
            .collect(toImmutableMap(RaftServerInfo::id, Function.identity()));
    return new RaftConfiguration(
        new RaftServerId(serverConfig.getThisServerId()),
        clusterServers,
        new RaftTimerInterval(
            raftConfig.getTimerMinimumMilliseconds(), raftConfig.getTimerMaximumMilliseconds()));
  }

  private static class ServerInfoConverter
      extends Converter<ServerConfig.ServerInfo, RaftServerInfo> {

    private static final ServerInfoConverter INSTANCE = new ServerInfoConverter();

    @Override
    protected RaftServerInfo doForward(ServerConfig.ServerInfo serverInfo) {
      return new RaftServerInfo(
          new RaftServerId(serverInfo.getServerId()),
          serverInfo.getHost(),
          serverInfo.getRespPort());
    }

    @Override
    protected ServerConfig.ServerInfo doBackward(RaftServerInfo raftServerInfo) {
      return ServerConfig.ServerInfo.newBuilder()
          .setServerId(raftServerInfo.id().toString())
          .setHost(raftServerInfo.host())
          .setRespPort(raftServerInfo.port())
          .build();
    }
  }
}
