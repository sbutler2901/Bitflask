package dev.sbutler.bitflask.config;

final class ConfigValidator {

  static void validate(BitflaskConfig bitflaskConfig) {
    validateServerConfig(bitflaskConfig.getServerConfig());
    validateStorageConfig(bitflaskConfig.getStorageConfig());
    validateRaftConfig(bitflaskConfig.getRaftConfig());
  }

  static void validateServerConfig(ServerConfig serverConfig) {}

  static void validateStorageConfig(StorageConfig storageConfig) {}

  static void validateRaftConfig(RaftConfig raftConfig) {}

  private ConfigValidator() {}
}
