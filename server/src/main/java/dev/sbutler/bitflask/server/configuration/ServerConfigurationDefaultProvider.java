package dev.sbutler.bitflask.server.configuration;

import dev.sbutler.bitflask.common.configuration.ConfigurationDefaultProvider;
import java.util.ResourceBundle;

/**
 * Manages provided the default value for Server configurations when a command line flag was not
 * passed.
 *
 * <p>If a ResourceBundle is provided, and it contains a value for a corresponding property, the
 * contained value will be provided. Otherwise, the hardcoded default value will be returned.
 */
public class ServerConfigurationDefaultProvider extends ConfigurationDefaultProvider {

  static final String SERVER_PORT_PROPERTY_KEY = "server.port";
  static final int DEFAULT_SERVER_PORT = 9090;

  public ServerConfigurationDefaultProvider() {
    super();
  }

  public ServerConfigurationDefaultProvider(ResourceBundle resourceBundle) {
    super(resourceBundle);
  }

  @Override
  public String getDefaultValueFor(String optionName) {
    return switch (optionName) {
      case ServerConfiguration.SERVER_PORT_FLAG_SHORT, ServerConfiguration.SERVER_PORT_FLAG_LONG ->
          getWithPropertyKeyOrDefault(SERVER_PORT_PROPERTY_KEY, DEFAULT_SERVER_PORT);
      default -> null;
    };
  }
}
