package dev.sbutler.bitflask.server.configuration;

import com.beust.jcommander.Parameter;
import dev.sbutler.bitflask.common.configuration.Configurations;
import dev.sbutler.bitflask.common.configuration.validators.PositiveIntegerValidator;

/**
 * Provides access to the server's runtime configurations.
 */
public class ServerConfigurations implements Configurations {

  @Parameter(names = {
      ServerConfigurationsConstants.SERVER_PORT_FLAG_SHORT,
      ServerConfigurationsConstants.SERVER_PORT_FLAG_LONG},
      validateWith = PositiveIntegerValidator.class,
      description = "Port that the server listens on for incoming connections")
  private int port;

  public int getPort() {
    return port;
  }

  @Override
  public String toString() {
    return "ServerConfigurations{" +
        "port=" + port +
        '}';
  }
}
