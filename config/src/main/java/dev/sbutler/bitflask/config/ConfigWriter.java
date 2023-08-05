package dev.sbutler.bitflask.config;

import com.google.common.collect.ImmutableMap;
import com.google.common.flogger.FluentLogger;
import com.google.protobuf.util.JsonFormat;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** CLI tool for building BitflaskConfig as a json file. */
public class ConfigWriter {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final String configFileName = "bitflask_config.json";

  public static void main(String[] args) throws Exception {
    BitflaskConfig bitflaskConfig = buildBitflaskConfig();
    String configJson = JsonFormat.printer().print(bitflaskConfig);

    Path configOutputDir = Paths.get(System.getProperty("user.home") + "/.bitflask/config");
    Path configOutputFile = configOutputDir.resolve(configFileName);

    Files.createDirectories(configOutputDir);
    Files.writeString(configOutputFile, configJson, StandardCharsets.UTF_8);

    logger.atInfo().log("Config writen to [%s]", configOutputFile.toAbsolutePath().toString());
    logger.atInfo().log(configJson);
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
