package dev.sbutler.bitflask.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

/** Provides bindings for Bitflask subproject configurations. */
public class ConfigModule extends AbstractModule {

  private final BitflaskConfig bitflaskConfig;

  public ConfigModule(BitflaskConfig bitflaskConfig) {
    this.bitflaskConfig = bitflaskConfig;
  }

  @Provides
  ServerConfig provideServerConfig() {
    return bitflaskConfig.getServerConfig();
  }

  @Provides
  StorageConfig provideStorageConfig() {
    return bitflaskConfig.getStorageConfig();
  }

  @Provides
  RaftConfig provideRaftconfig() {
    return bitflaskConfig.getRaftConfig();
  }
}
