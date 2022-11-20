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

  @Override
  protected void configure() {
    bind(ConfigurationFlagMap.class)
        .toInstance(ClientConfigurationsConstants.CLIENT_FLAG_TO_CONFIGURATION_MAP);
    bind(ResourceBundle.class)
        .toInstance(ResourceBundle.getBundle(CONFIG_RESOURCE_BUNDLE_BASE_NAME));
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
}
