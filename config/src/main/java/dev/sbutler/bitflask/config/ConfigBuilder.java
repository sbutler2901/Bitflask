package dev.sbutler.bitflask.config;

import com.google.common.collect.ImmutableMap;
import java.nio.file.Path;
import java.nio.file.Paths;

/** CLI tool for building BitflaskConfig as a json file. */
public class ConfigBuilder {

  public static void main(String[] args) {
    BitflaskConfig bitflaskConfig = buildBitflaskConfig();
    Path configOutputPath = Paths.get(System.getProperty("user.home") + "/.bitflask/config");
    // write to file
  }

  private static BitflaskConfig buildBitflaskConfig() {
    return BitflaskConfig.newBuilder()
        .setServerConfig(buildServerConfig())
        .setStorageConfig(buildStorageConfig())
        .setRaftConfig(buildRaftConfig())
        .build();
  }

  private static ServerConfig buildServerConfig() {
    String thisServerId = "server_0";
    ImmutableMap<String, ServerConfig.ServerInfo> bitflaskServers =
        ImmutableMap.<String, ServerConfig.ServerInfo>builder()
            .put(
                thisServerId,
                ServerConfig.ServerInfo.newBuilder()
                    .setServerId(thisServerId)
                    .setHost("localhost")
                    .setRespPort(9090)
                    .build())
            .put(
                "server_1",
                ServerConfig.ServerInfo.newBuilder()
                    .setServerId("server_1")
                    .setHost("localhost")
                    .setRespPort(9091)
                    .build())
            .put(
                "server_2",
                ServerConfig.ServerInfo.newBuilder()
                    .setServerId("server_2")
                    .setHost("localhost")
                    .setRespPort(9091)
                    .build())
            .build();
    return ServerConfig.newBuilder()
        .setThisServerId(thisServerId)
        .putAllBitflaskServers(bitflaskServers)
        .build();
  }

  private static StorageConfig buildStorageConfig() {
    return StorageConfig.newBuilder()
        .setStoreDirectoryPath(
            Paths.get(System.getProperty("user.home") + "/.bitflask/store/").toString())
        .setLoadingMode(StorageConfig.LoadingMode.LOAD)
        .setMemtableFlushThresholdBytes(1048576) // 1 MiB
        .setSegmentLevelFlushThresholdBytes(5242880) // 5 MiB
        .setCompactorExecutionDelayMilliseconds(5000) // 5 seconds
        .build();
  }

  private static RaftConfig buildRaftConfig() {
    return RaftConfig.newBuilder()
        .setTimerMinimumMilliseconds(150)
        .setTimerMaximumMilliseconds(300)
        .build();
  }
}
