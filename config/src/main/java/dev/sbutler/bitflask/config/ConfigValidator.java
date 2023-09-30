package dev.sbutler.bitflask.config;

import dev.sbutler.bitflask.config.validators.AbsolutePathValidator;
import dev.sbutler.bitflask.config.validators.NonBlankStringValidator;
import dev.sbutler.bitflask.config.validators.PositiveIntegerValidator;
import dev.sbutler.bitflask.config.validators.PositiveLongValidator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Handles validating the correctness of {@link BitflaskConfig}. */
final class ConfigValidator {

  private static final PositiveIntegerValidator positiveIntegerValidator =
      new PositiveIntegerValidator();
  private static final PositiveLongValidator positiveLongValidator = new PositiveLongValidator();
  private static final NonBlankStringValidator nonBlankStringValidator =
      new NonBlankStringValidator();
  private static final AbsolutePathValidator absolutePathValidator = new AbsolutePathValidator();

  static void validate(BitflaskConfig bitflaskConfig) {
    validateServerConfig(bitflaskConfig.getServerConfig());
    validateStorageConfig(bitflaskConfig.getStorageConfig());
    validateRaftConfig(bitflaskConfig.getRaftConfig());
  }

  static void validateForWrite(BitflaskConfig bitflaskConfig) {
    validateServerConfigForWrite(bitflaskConfig.getServerConfig());
    validateStorageConfig(bitflaskConfig.getStorageConfig());
    validateRaftConfig(bitflaskConfig.getRaftConfig());
  }

  static void validateServerConfigForWrite(ServerConfig serverConfig) {
    validateUniqueServerInfo(serverConfig.getBitflaskServersList());
    serverConfig.getBitflaskServersList().forEach(ConfigValidator::validateServerInfo);
  }

  static void validateServerConfig(ServerConfig serverConfig) {
    validateUniqueServerInfo(serverConfig.getBitflaskServersList());
    serverConfig.getBitflaskServersList().forEach(ConfigValidator::validateServerInfo);

    String thisServerId = serverConfig.getThisServerId();
    nonBlankStringValidator.validate("this_server_id", thisServerId);
    if (serverConfig.getBitflaskServersList().stream()
        .noneMatch(serverInfo -> thisServerId.equals(serverInfo.getServerId()))) {
      throw new InvalidConfigurationException(
          String.format(
              "this_server_id [%s] not found in bitflask_servers: %s",
              thisServerId, serverConfig.getBitflaskServersList()));
    }
  }

  private static void validateUniqueServerInfo(List<ServerConfig.ServerInfo> serverInfoList) {
    Set<ServerConfig.ServerInfo> uniqueServerInfo = new HashSet<>();
    for (var info : serverInfoList) {
      if (!uniqueServerInfo.add(info)) {
        throw new InvalidConfigurationException(
            String.format("Duplicate ServerInfo instance found:\n[%s]", info));
      }
    }
  }

  private static void validateServerInfo(ServerConfig.ServerInfo serverInfo) {
    nonBlankStringValidator.validate("server_id", serverInfo.getServerId());
    nonBlankStringValidator.validate("host", serverInfo.getHost());
    positiveIntegerValidator.validate("resp_port", serverInfo.getRespPort());
  }

  static void validateStorageConfig(StorageConfig storageConfig) {
    absolutePathValidator.validate("store_directory_path", storageConfig.getStoreDirectoryPath());
    positiveLongValidator.validate(
        "memtable_flush_threshold_bytes", storageConfig.getMemtableFlushThresholdBytes());
    positiveLongValidator.validate(
        "segment_level_flush_threshold_bytes", storageConfig.getSegmentLevelFlushThresholdBytes());
    positiveIntegerValidator.validate(
        "compactor_execution_delay_milliseconds",
        storageConfig.getCompactorExecutionDelayMilliseconds());
  }

  static void validateRaftConfig(RaftConfig raftConfig) {
    positiveIntegerValidator.validate(
        "timer_minimum_milliseconds", raftConfig.getTimerMinimumMilliseconds());
    positiveIntegerValidator.validate(
        "timer_maximum_milliseconds", raftConfig.getTimerMaximumMilliseconds());
  }

  private ConfigValidator() {}
}
