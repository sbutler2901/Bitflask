package dev.sbutler.bitflask.storage.raft;

import static com.google.common.truth.Truth.assertThat;

import dev.sbutler.bitflask.config.RaftConfig;
import dev.sbutler.bitflask.config.ServerConfig;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link RaftConfigurationProvider}. */
public class RaftConfigurationProviderTest {

  private static final RaftServerId thisServerId = new RaftServerId("this-server");
  private static final ServerConfig SERVER_CONFIG =
      ServerConfig.newBuilder()
          .setThisServerId(thisServerId.id())
          .addBitflaskServers(
              ServerConfig.ServerInfo.newBuilder()
                  .setServerId(thisServerId.id())
                  .setHost("localhost")
                  .setRespPort(9090)
                  .setRaftPort(9080)
                  .build())
          .buildPartial();
  private static final RaftConfig RAFT_CONFIG =
      RaftConfig.newBuilder()
          .setTimerMinimumMilliseconds(0)
          .setTimerMaximumMilliseconds(100)
          .buildPartial();

  private final RaftConfigurationProvider configurationProvider =
      new RaftConfigurationProvider(SERVER_CONFIG, RAFT_CONFIG);

  @Test
  public void get() {
    RaftConfiguration configuration = configurationProvider.get();

    assertThat(configuration.thisRaftServerId()).isEqualTo(thisServerId);
    assertThat(configuration.clusterServers().containsKey(thisServerId)).isTrue();
    assertThat(configuration.clusterServers().get(thisServerId))
        .isEqualTo(SERVER_CONFIG.getBitflaskServers(0));
    assertThat(configuration.raftTimerInterval().minimumMilliSeconds()).isEqualTo(0);
    assertThat(configuration.raftTimerInterval().maximumMilliseconds()).isEqualTo(100);
    // Ensure memoized
    assertThat(configuration).isEqualTo(configurationProvider.get());
  }
}
