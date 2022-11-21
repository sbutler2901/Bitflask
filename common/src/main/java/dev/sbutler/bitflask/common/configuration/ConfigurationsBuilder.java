package dev.sbutler.bitflask.common.configuration;

import com.beust.jcommander.JCommander;
import java.util.ResourceBundle;

/**
 * Builds {@link dev.sbutler.bitflask.common.configuration.Configurations} from command line
 * arguments and config files expressed as {@link ResourceBundle}s.
 */
public class ConfigurationsBuilder {

  private final String[] args;
  private final ResourceBundle resourceBundle;

  public ConfigurationsBuilder(String[] args, ResourceBundle resourceBundle) {
    this.args = args;
    this.resourceBundle = resourceBundle;
  }

  public void build(Configurations configurations, ConfigurationFlagMap flagMap) {
    ConfigurationDefaultProvider defaultProvider =
        new ConfigurationDefaultProvider(flagMap, resourceBundle);
    JCommander.newBuilder()
        .addObject(configurations)
        .defaultProvider(defaultProvider)
        .acceptUnknownOptions(false)
        .build()
        .parse(args);
  }

  public void buildAcceptingUnknownOptions(Configurations configurations,
      ConfigurationFlagMap flagMap) {
    ConfigurationDefaultProvider defaultProvider =
        new ConfigurationDefaultProvider(flagMap, resourceBundle);
    JCommander.newBuilder()
        .addObject(configurations)
        .defaultProvider(defaultProvider)
        .acceptUnknownOptions(true)
        .build()
        .parse(args);
  }
}
