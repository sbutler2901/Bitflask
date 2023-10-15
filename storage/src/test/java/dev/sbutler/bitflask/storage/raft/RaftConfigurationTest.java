package dev.sbutler.bitflask.storage.raft;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;
import dev.sbutler.bitflask.config.ServerConfig;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link RaftConfiguration}. */
public class RaftConfigurationTest {
  private static final RaftServerId thisServerId = new RaftServerId("this-server");
  private static final RaftServerId otherServerId = new RaftServerId("other-server");
  private static final ServerConfig.ServerInfo thisServerConfig =
      ServerConfig.ServerInfo.newBuilder()
          .setServerId(thisServerId.id())
          .setHost("localhost")
          .setRespPort(9090)
          .setRaftPort(9080)
          .build();
  private static final ServerConfig.ServerInfo otherServerConfig =
      ServerConfig.ServerInfo.newBuilder()
          .setServerId(otherServerId.id())
          .setHost("localhost")
          .setRespPort(9091)
          .setRaftPort(9081)
          .build();
  private static final ImmutableMap<RaftServerId, ServerConfig.ServerInfo> clusterServer =
      ImmutableMap.of(thisServerId, thisServerConfig, otherServerId, otherServerConfig);
  private static final RaftTimerInterval timerInterval = new RaftTimerInterval(0, 100);

  private final RaftConfiguration raftConfiguration =
      new RaftConfiguration(thisServerId, clusterServer, timerInterval);

  @Test
  public void getOtherServersInCluster() {
    var otherServers = raftConfiguration.getOtherServersInCluster();

    assertThat(otherServers.containsKey(thisServerId)).isFalse();
  }

  @Test
  public void getThisServerInfo() {
    assertThat(raftConfiguration.getThisServerInfo()).isEqualTo(clusterServer.get(thisServerId));
  }
}
