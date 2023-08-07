package dev.sbutler.bitflask.storage.raft;

import com.google.common.base.Converter;
import dev.sbutler.bitflask.config.ServerConfig;

/** Handles converting between {@link ServerConfig.ServerInfo} and {@link RaftServerInfo}. */
final class RaftServerInfoConverter extends Converter<ServerConfig.ServerInfo, RaftServerInfo> {

  public static final RaftServerInfoConverter INSTANCE = new RaftServerInfoConverter();

  @Override
  protected RaftServerInfo doForward(ServerConfig.ServerInfo serverInfo) {
    return new RaftServerInfo(
        new RaftServerId(serverInfo.getServerId()), serverInfo.getHost(), serverInfo.getRespPort());
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
