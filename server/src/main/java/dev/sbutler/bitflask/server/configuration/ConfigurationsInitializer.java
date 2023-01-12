package dev.sbutler.bitflask.server.configuration;

import dev.sbutler.bitflask.common.configuration.ConfigurationsBuilder;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurationsConstants;

/**
 * Handles initializing the configurations required to run the server
 */
public final class ConfigurationsInitializer {

  private final ConfigurationsBuilder configurationsBuilder;

  public ConfigurationsInitializer(ConfigurationsBuilder configurationsBuilder) {
    this.configurationsBuilder = configurationsBuilder;
  }

  public InitializedConfigurations initializeConfigurations() {
    return new InitializedConfigurations(
        initializeServerConfigurations(),
        initializeStorageConfigurations());
  }

  private ServerConfigurations initializeServerConfigurations() {
    ServerConfigurations serverConfigurations = new ServerConfigurations();
    configurationsBuilder.buildAcceptingUnknownOptions(
        serverConfigurations,
        ServerConfigurationsConstants.SERVER_FLAG_TO_CONFIGURATION_MAP);
    return serverConfigurations;
  }

  private StorageConfigurations initializeStorageConfigurations() {
    StorageConfigurations storageConfigurations = new StorageConfigurations();
    configurationsBuilder.buildAcceptingUnknownOptions(storageConfigurations,
        StorageConfigurationsConstants.STORAGE_FLAG_TO_CONFIGURATION_MAP);
    return storageConfigurations;
  }

  public record InitializedConfigurations(ServerConfigurations serverConfigurations,
                                          StorageConfigurations storageConfigurations) {

  }
}
