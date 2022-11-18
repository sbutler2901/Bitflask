package dev.sbutler.bitflask.common.configuration;

import com.beust.jcommander.IDefaultProvider;
import java.util.ResourceBundle;

/**
 * Provides core functionality utilized by ConfigurationDefaultProviders
 */
public class ConfigurationDefaultProvider implements IDefaultProvider {

  private final ConfigurationFlagMap flagMap;
  private final ResourceBundle resourceBundle;

  public ConfigurationDefaultProvider(ConfigurationFlagMap flagMap) {
    this.flagMap = flagMap;
    this.resourceBundle = null;
  }

  public ConfigurationDefaultProvider(
      ConfigurationFlagMap flagMap,
      ResourceBundle resourceBundle) {
    this.flagMap = flagMap;
    this.resourceBundle = resourceBundle;
  }

  @Override
  public String getDefaultValueFor(String flag) {
    Configuration configuration = flagMap.get(flag);
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
