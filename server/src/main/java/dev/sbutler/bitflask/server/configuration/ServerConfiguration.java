package dev.sbutler.bitflask.server.configuration;

import com.beust.jcommander.Parameter;
import dev.sbutler.bitflask.common.configuration.validators.PositiveIntegerValidator;
import java.util.ResourceBundle;

/**
 * Provides access to the server's runtime configurations.
 *
 * <p>The configuration parameters can be set via command line flags or a property file. The
 * priority for defining the parameters:
 * <ol>
 *   <li>command line flags</li>
 *   <li>property file</li>
 *   <li>hardcoded value</li>
 * </ol>
 */
public class ServerConfiguration {

  @Parameter(names = {"-p", "--port"},
      validateWith = PositiveIntegerValidator.class,
      description = "Port that the server listens on for incoming connections")
  private int port = 9090;

  public ServerConfiguration() {
  }

  public ServerConfiguration(ResourceBundle resourceBundle) {
    if (resourceBundle.containsKey("server.port")) {
      port = Integer.parseInt(resourceBundle.getString("server.port"));
    }
  }

  public int getPort() {
    return port;
  }
}
