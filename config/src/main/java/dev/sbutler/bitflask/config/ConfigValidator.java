package dev.sbutler.bitflask.config;

import dev.sbutler.bitflask.config.validators.AbsolutePathValidator;
import dev.sbutler.bitflask.config.validators.NonBlankStringValidator;
import dev.sbutler.bitflask.config.validators.PositiveIntegerValidator;
import dev.sbutler.bitflask.config.validators.PositiveLongValidator;
import java.util.Map;

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

  static void validateServerConfig(ServerConfig serverConfig) {
    String thisServerId = serverConfig.getThisServerId();
    nonBlankStringValidator.validate("this_server_id", thisServerId);
    if (!serverConfig.containsBitflaskServers(thisServerId)) {
      throw new InvalidConfigurationException(
          String.format(
              "this_server_id [%s] not found in bitflask_servers: %s",
              thisServerId, serverConfig.getBitflaskServersMap().values()));
    }
    for (Map.Entry<String, ServerConfig.ServerInfo> entry :
        serverConfig.getBitflaskServersMap().entrySet()) {
      nonBlankStringValidator.validate("key: server_id", entry.getKey());
      nonBlankStringValidator.validate("value: server_id", entry.getValue().getServerId());
      if (!entry.getKey().equals(entry.getValue().getServerId())) {
        throw new InvalidConfigurationException(
            String.format(
                "bitflask_server key [%s] does not match ServerInfo.server_id [%s]",
                entry.getKey(), entry.getValue().getServerId()));
      }
      nonBlankStringValidator.validate("host", entry.getValue().getHost());
      positiveIntegerValidator.validate("resp_port", entry.getValue().getRespPort());
    }
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
