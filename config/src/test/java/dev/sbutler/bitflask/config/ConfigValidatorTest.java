package dev.sbutler.bitflask.config;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link ConfigValidator}. */
public class ConfigValidatorTest {

  @Test
  public void serverConfig_valid() {
    ServerConfig serverConfig =
        ServerConfig.newBuilder()
            .setThisServerId("server_0")
            .putBitflaskServers(
                "server_0",
                ServerConfig.ServerInfo.newBuilder()
                    .setServerId("server_0")
                    .setHost("host")
                    .setRespPort(1)
                    .build())
            .build();

    try {
      ConfigValidator.validateServerConfig(serverConfig);
    } catch (Exception e) {
      fail(e);
    }
  }

  @Test
  public void serverConfig_thisServerId_invalid() {
    ServerConfig serverConfig = ServerConfig.newBuilder().setThisServerId("").buildPartial();

    InvalidConfigurationException e =
        assertThrows(
            InvalidConfigurationException.class,
            () -> ConfigValidator.validateServerConfig(serverConfig));

    assertThat(e).hasMessageThat().contains("this_server_id");
  }

  @Test
  public void serverConfig_thisServerId_notInBitflaskServers() {
    ServerConfig serverConfig =
        ServerConfig.newBuilder().setThisServerId("server_0").buildPartial();

    InvalidConfigurationException e =
        assertThrows(
            InvalidConfigurationException.class,
            () -> ConfigValidator.validateServerConfig(serverConfig));

    assertThat(e).hasMessageThat().contains("this_server_id");
    assertThat(e).hasMessageThat().contains("not found");
  }

  @Test
  public void serverConfig_bitflaskServers_invalidKey() {
    ServerConfig serverConfig =
        ServerConfig.newBuilder()
            .setThisServerId("server_0")
            .putBitflaskServers("", ServerConfig.ServerInfo.getDefaultInstance())
            .putBitflaskServers("server_0", ServerConfig.ServerInfo.getDefaultInstance())
            .build();

    InvalidConfigurationException e =
        assertThrows(
            InvalidConfigurationException.class,
            () -> ConfigValidator.validateServerConfig(serverConfig));

    assertThat(e).hasMessageThat().contains("key: server_id");
  }

  @Test
  public void serverConfig_bitflaskServers_serverInfo_invalidServerId() {
    ServerConfig serverConfig =
        ServerConfig.newBuilder()
            .setThisServerId("server_0")
            .putBitflaskServers(
                "server_0",
                ServerConfig.ServerInfo.newBuilder()
                    .setServerId("")
                    .setHost("host")
                    .setRespPort(1)
                    .build())
            .build();

    InvalidConfigurationException e =
        assertThrows(
            InvalidConfigurationException.class,
            () -> ConfigValidator.validateServerConfig(serverConfig));

    assertThat(e).hasMessageThat().contains("value: server_id");
  }

  @Test
  public void serverConfig_bitflaskServers_serverInfo_keyAndServerIdMismatch() {
    ServerConfig serverConfig =
        ServerConfig.newBuilder()
            .setThisServerId("server_0")
            .putBitflaskServers(
                "server_0",
                ServerConfig.ServerInfo.newBuilder()
                    .setServerId("server_1")
                    .setHost("host")
                    .setRespPort(1)
                    .build())
            .build();

    InvalidConfigurationException e =
        assertThrows(
            InvalidConfigurationException.class,
            () -> ConfigValidator.validateServerConfig(serverConfig));

    assertThat(e).hasMessageThat().contains("does not match");
  }

  @Test
  public void serverConfig_bitflaskServers_serverInfo_invalidHost() {
    ServerConfig serverConfig =
        ServerConfig.newBuilder()
            .setThisServerId("server_0")
            .putBitflaskServers(
                "server_0",
                ServerConfig.ServerInfo.newBuilder()
                    .setServerId("server_0")
                    .setHost("")
                    .setRespPort(1)
                    .build())
            .build();

    InvalidConfigurationException e =
        assertThrows(
            InvalidConfigurationException.class,
            () -> ConfigValidator.validateServerConfig(serverConfig));

    assertThat(e).hasMessageThat().contains("host");
  }

  @Test
  public void serverConfig_bitflaskServers_serverInfo_invalidPort() {
    ServerConfig serverConfig =
        ServerConfig.newBuilder()
            .setThisServerId("server_0")
            .putBitflaskServers(
                "server_0",
                ServerConfig.ServerInfo.newBuilder()
                    .setServerId("server_0")
                    .setHost("host")
                    .setRespPort(0)
                    .build())
            .build();

    InvalidConfigurationException e =
        assertThrows(
            InvalidConfigurationException.class,
            () -> ConfigValidator.validateServerConfig(serverConfig));

    assertThat(e).hasMessageThat().contains("resp_port");
  }

  @Test
  public void storageConfig_valid() {
    StorageConfig storageConfig =
        StorageConfig.newBuilder()
            .setStoreDirectoryPath("/tmp")
            .setMemtableFlushThresholdBytes(1L)
            .setSegmentLevelFlushThresholdBytes(1L)
            .setCompactorExecutionDelayMilliseconds(1)
            .build();

    ConfigValidator.validateStorageConfig(storageConfig);
  }

  @Test
  public void storageConfig_storeDirectoryPath_invalid() {
    StorageConfig storageConfig =
        StorageConfig.newBuilder().setStoreDirectoryPath("~/").buildPartial();

    InvalidConfigurationException e =
        assertThrows(
            InvalidConfigurationException.class,
            () -> ConfigValidator.validateStorageConfig(storageConfig));

    assertThat(e).hasMessageThat().contains("store_directory_path");
  }

  @Test
  public void storageConfig_memtableFlushThresholdBytes_invalid() {
    StorageConfig storageConfig =
        StorageConfig.newBuilder()
            .setStoreDirectoryPath("/tmp")
            .setMemtableFlushThresholdBytes(0L)
            .buildPartial();

    InvalidConfigurationException e =
        assertThrows(
            InvalidConfigurationException.class,
            () -> ConfigValidator.validateStorageConfig(storageConfig));

    assertThat(e).hasMessageThat().contains("memtable_flush_threshold_bytes");
  }

  @Test
  public void storageConfig_segmentLevelFlushThresholdBytes_invalid() {
    StorageConfig storageConfig =
        StorageConfig.newBuilder()
            .setStoreDirectoryPath("/tmp")
            .setMemtableFlushThresholdBytes(1L)
            .setSegmentLevelFlushThresholdBytes(0L)
            .buildPartial();

    InvalidConfigurationException e =
        assertThrows(
            InvalidConfigurationException.class,
            () -> ConfigValidator.validateStorageConfig(storageConfig));

    assertThat(e).hasMessageThat().contains("segment_level_flush_threshold_bytes");
  }

  @Test
  public void storageConfig_compactorExecutionDelay_milliseconds_invalid() {
    StorageConfig storageConfig =
        StorageConfig.newBuilder()
            .setStoreDirectoryPath("/tmp")
            .setMemtableFlushThresholdBytes(1L)
            .setSegmentLevelFlushThresholdBytes(1L)
            .setCompactorExecutionDelayMilliseconds(0)
            .buildPartial();

    InvalidConfigurationException e =
        assertThrows(
            InvalidConfigurationException.class,
            () -> ConfigValidator.validateStorageConfig(storageConfig));

    assertThat(e).hasMessageThat().contains("compactor_execution_delay_milliseconds");
  }

  @Test
  public void raftConfig_success() {
    RaftConfig raftConfig =
        RaftConfig.newBuilder()
            .setTimerMinimumMilliseconds(1)
            .setTimerMaximumMilliseconds(1)
            .build();

    ConfigValidator.validateRaftConfig(raftConfig);
  }

  @Test
  public void raftConfig_timerMinimumMilliseconds_invalid() {
    RaftConfig raftConfig = RaftConfig.newBuilder().setTimerMinimumMilliseconds(0).buildPartial();

    InvalidConfigurationException e =
        assertThrows(
            InvalidConfigurationException.class,
            () -> ConfigValidator.validateRaftConfig(raftConfig));

    assertThat(e).hasMessageThat().contains("timer_minimum_milliseconds");
  }

  @Test
  public void raftConfig_timerMaximumMilliseconds_invalid() {
    RaftConfig raftConfig =
        RaftConfig.newBuilder()
            .setTimerMinimumMilliseconds(1)
            .setTimerMaximumMilliseconds(0)
            .buildPartial();

    InvalidConfigurationException e =
        assertThrows(
            InvalidConfigurationException.class,
            () -> ConfigValidator.validateRaftConfig(raftConfig));

    assertThat(e).hasMessageThat().contains("timer_maximum_milliseconds");
  }
}
