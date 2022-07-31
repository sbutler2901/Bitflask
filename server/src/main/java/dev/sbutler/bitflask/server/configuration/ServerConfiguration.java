package dev.sbutler.bitflask.server.configuration;

import com.beust.jcommander.Parameter;
import dev.sbutler.bitflask.common.configuration.validators.PositiveIntegerValidator;

/**
 * Provides access to the server's runtime configurations.
 *
 * <p>It is required that this class is initialized using {@link com.beust.jcommander.JCommander}
 * accompanied by the default provided {@link ServerConfigurationDefaultProvider}.
 *
 * <p>The configuration parameters can be set via command line flags or a property file. The
 * priority order for defining the parameters is:
 * <ol>
 *   <li>command line flags</li>
 *   <li>property file</li>
 *   <li>hardcoded value</li>
 * </ol>
 *
 * <p>Note: an illegal parameter value will cause an
 * {@link dev.sbutler.bitflask.common.configuration.exceptions.IllegalConfigurationException} to be
 * thrown WITHOUT falling back to a lower priority parameter definition. For example, a negative
 * number provided for the server port via command line will throw an exception
 * rather than referring to the property file, or hardcoded value.
 */
public class ServerConfiguration {

  static final String SERVER_PORT_FLAG_SHORT = "-p";
  static final String SERVER_PORT_FLAG_LONG = "--port";

  @Parameter(names = {SERVER_PORT_FLAG_SHORT, SERVER_PORT_FLAG_LONG},
      validateWith = PositiveIntegerValidator.class,
      description = "Port that the server listens on for incoming connections")
  private int port;

  public int getPort() {
    return port;
  }
}
