package dev.sbutler.bitflask.client.configuration;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.sbutler.bitflask.common.configuration.ConfigurationFlagMap;
import dev.sbutler.bitflask.common.configuration.ConfigurationsBuilder;
import java.util.ResourceBundle;
import javax.inject.Singleton;

public class ConfigurationModule extends AbstractModule {

  private static final String CONFIG_RESOURCE_BUNDLE_BASE_NAME = "config";

  private final String[] args;

  public ConfigurationModule(String[] args) {
    this.args = args;
  }

  @Provides
  @Singleton
  ClientConfigurations provideClientConfiguration(ConfigurationsBuilder configsBuilder,
      ConfigurationFlagMap flagMap) {
    ClientConfigurations configuration = new ClientConfigurations();
    configsBuilder.build(configuration, flagMap);
    return configuration;
  }

  @Provides
  ConfigurationsBuilder provideConfigurationsBuilder(ResourceBundle resourceBundle) {
    return new ConfigurationsBuilder(args, resourceBundle);
  }

  @Provides
  ConfigurationFlagMap provideConfigurationFlagMap() {
    return ClientConfigurationsConstants.CLIENT_FLAG_TO_CONFIGURATION_MAP;
  }

  @Provides
  @Singleton
  ResourceBundle provideResourceBundle() {
    return ResourceBundle.getBundle(CONFIG_RESOURCE_BUNDLE_BASE_NAME);
  }
}
