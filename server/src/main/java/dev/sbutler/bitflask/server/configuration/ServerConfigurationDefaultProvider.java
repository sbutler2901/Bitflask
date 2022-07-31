package dev.sbutler.bitflask.server.configuration;

import com.beust.jcommander.IDefaultProvider;
import java.util.ResourceBundle;

/**
 * Manages provided the default value for Server configurations when a command line flag was not
 * passed.
 *
 * <p>If a ResourceBundle is provided, and it contains a value for a corresponding property, the
 * contained value will be provided. Otherwise, the hardcoded default value will be returned.
 */
public class ServerConfigurationDefaultProvider implements IDefaultProvider {

  static final String SERVER_PORT_PROPERTY_KEY = "server.port";
  static final int DEFAULT_SERVER_PORT = 9090;

  private final ResourceBundle resourceBundle;

  public ServerConfigurationDefaultProvider() {
    this.resourceBundle = null;
  }

  public ServerConfigurationDefaultProvider(ResourceBundle resourceBundle) {
    this.resourceBundle = resourceBundle;
  }

  @Override
  public String getDefaultValueFor(String optionName) {
    return switch (optionName) {
      case ServerConfiguration.SERVER_PORT_FLAG_SHORT, ServerConfiguration.SERVER_PORT_FLAG_LONG ->
          getServerPort();
      default -> null;
    };
  }

  private String getServerPort() {
    if (resourceBundle == null || !resourceBundle.containsKey(SERVER_PORT_PROPERTY_KEY)) {
      return String.valueOf(DEFAULT_SERVER_PORT);
    } else {
      return resourceBundle.getString(SERVER_PORT_PROPERTY_KEY);
    }
  }
}
