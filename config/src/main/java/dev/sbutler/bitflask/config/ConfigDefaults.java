package dev.sbutler.bitflask.config;

import com.google.common.collect.ImmutableList;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ConfigDefaults {

  static final Path CONFIG_OUTPUT_DIR =
      Paths.get(System.getProperty("user.home") + "/.bitflask/config");
  static final String CONFIG_FILE_NAME = "bitflask_config.json";
  static final Path CONFIG_OUTPUT_FILE = CONFIG_OUTPUT_DIR.resolve(CONFIG_FILE_NAME);

  public static ServerConfig.ServerInfo SERVER_INFO_0 =
      ServerConfig.ServerInfo.newBuilder()
          .setServerId("server_0")
          .setHost("localhost")
          .setRespPort(9090)
          .setRaftPort(9080)
          .build();
  public static ServerConfig.ServerInfo SERVER_INFO_1 =
      ServerConfig.ServerInfo.newBuilder()
          .setServerId("server_1")
          .setHost("localhost")
          .setRespPort(9091)
          .setRaftPort(9081)
          .build();
  public static ServerConfig.ServerInfo SERVER_INFO_2 =
      ServerConfig.ServerInfo.newBuilder()
          .setServerId("server_2")
          .setHost("localhost")
          .setRespPort(9092)
          .setRaftPort(9082)
          .build();

  public static ServerConfig SERVER_CONFIG =
      ServerConfig.newBuilder()
          .addAllBitflaskServers(ImmutableList.of(SERVER_INFO_0, SERVER_INFO_1, SERVER_INFO_2))
          .buildPartial();

  public static StorageConfig STORAGE_CONFIG =
      StorageConfig.newBuilder()
          .setStoreDirectoryPath(
              Paths.get(System.getProperty("user.home") + "/.bitflask/store/").toString())
          .setLoadingMode(StorageConfig.LoadingMode.TRUNCATE)
          .setMemtableFlushThresholdBytes(1048576) // 1 MiB
          .setSegmentLevelFlushThresholdBytes(5242880) // 5 MiB
          .setCompactorExecutionDelayMilliseconds(5000) // 5 seconds
          .build();

  public static RaftConfig RAFT_CONFIG =
      RaftConfig.newBuilder()
          .setTimerMinimumMilliseconds(150)
          .setTimerMaximumMilliseconds(300)
          .build();

  public static BitflaskConfig BITFLASK_CONFIG =
      BitflaskConfig.newBuilder()
          .setServerConfig(SERVER_CONFIG)
          .setStorageConfig(STORAGE_CONFIG)
          .setRaftConfig(RAFT_CONFIG)
          .build();

  private ConfigDefaults() {}
}
