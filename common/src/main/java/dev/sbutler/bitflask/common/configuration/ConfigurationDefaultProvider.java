package dev.sbutler.bitflask.common.configuration;

import com.beust.jcommander.IDefaultProvider;
import java.util.ResourceBundle;

/**
 * Provides core functionality utilized by ConfigurationDefaultProviders
 */
public abstract class ConfigurationDefaultProvider implements IDefaultProvider {

  protected final ResourceBundle resourceBundle;

  public ConfigurationDefaultProvider() {
    this.resourceBundle = null;
  }

  public ConfigurationDefaultProvider(ResourceBundle resourceBundle) {
    this.resourceBundle = resourceBundle;
  }

  /**
   * Return the value for the provided propertyKey from the resourceBundle, or the defaultValue.
   *
   * <p>The default value will be returned if a ResourceBundle was not provided, or a value for the
   * provided propertyKey was not found.
   */
  protected String getWithPropertyKeyOrDefault(String propertyKey, Object defaultValue) {
    if (resourceBundle == null
        || !resourceBundle.containsKey(propertyKey)) {
      return defaultValue.toString();
    } else {
      return resourceBundle.getString(propertyKey);
    }
  }
}
