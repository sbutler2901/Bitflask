package dev.sbutler.bitflask.common.configuration;

import com.beust.jcommander.IDefaultProvider;
import com.google.common.collect.ImmutableMap;
import java.util.ResourceBundle;

/**
 * Provides core functionality utilized by ConfigurationDefaultProviders
 */
public class ConfigurationDefaultProvider implements IDefaultProvider {

  private final ImmutableMap<String, Configuration> flagToConfigurationMap;
  private final ResourceBundle resourceBundle;

  public ConfigurationDefaultProvider(ImmutableMap<String, Configuration> flagToConfigurationMap) {
    this.flagToConfigurationMap = flagToConfigurationMap;
    this.resourceBundle = null;
  }

  public ConfigurationDefaultProvider(
      ImmutableMap<String, Configuration> flagToConfigurationMap,
      ResourceBundle resourceBundle) {
    this.flagToConfigurationMap = flagToConfigurationMap;
    this.resourceBundle = resourceBundle;
  }

  @Override
  public String getDefaultValueFor(String flag) {
    Configuration configuration = flagToConfigurationMap.get(flag);
    if (configuration == null) {
      return null;
    }
    return getFromResourceBundleOrDefault(configuration);
  }

  /**
   * Return the value for the provided ConfigurationDefault's propertyKey from the resource bundle,
   * or the ConfigurationDefault's defaultValue.
   *
   * <p>The default value will be returned if a ResourceBundle was not provided, or a value for the
   * provided propertyKey was not found.
   */
  private String getFromResourceBundleOrDefault(Configuration configuration) {
    if (resourceBundle == null
        || !resourceBundle.containsKey(configuration.propertyKey())) {
      return configuration.defaultValue().toString();
    } else {
      return resourceBundle.getString(configuration.propertyKey());
    }
  }
}
