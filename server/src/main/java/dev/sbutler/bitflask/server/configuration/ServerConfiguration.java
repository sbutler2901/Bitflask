package dev.sbutler.bitflask.server.configuration;

import com.beust.jcommander.Parameter;
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
      description = "Port that the server listens on for incoming connections")
  private int port = 9090;

  public ServerConfiguration(ResourceBundle resourceBundle) {
    if (resourceBundle.containsKey("port")) {
      port = Integer.parseInt(resourceBundle.getString("port"));
    }
  }

  public int getPort() {
    return port;
  }
}
