package dev.sbutler.bitflask.client.configuration;

import com.beust.jcommander.Parameter;
import dev.sbutler.bitflask.common.configuration.Configurations;
import dev.sbutler.bitflask.common.configuration.validators.NonBlankStringValidator;
import dev.sbutler.bitflask.common.configuration.validators.PositiveIntegerValidator;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides access to the server's runtime configurations.
 */
public class ClientConfiguration implements Configurations {

  @Parameter(description = "inline command")
  private List<String> inlineCommand = new ArrayList<>();

  @Parameter(names = {
      ClientConfigurationConstants.SERVER_HOST_FLAG_SHORT,
      ClientConfigurationConstants.SERVER_HOST_FLAG_LONG},
      validateWith = NonBlankStringValidator.class,
      description = "The host running the Bitflask server"
  )
  private String host;

  @Parameter(names = {
      ClientConfigurationConstants.SERVER_PORT_FLAG_SHORT,
      ClientConfigurationConstants.SERVER_PORT_FLAG_LONG},
      validateWith = PositiveIntegerValidator.class,
      description = "The port that the Bitflask server is listening on"
  )
  private int port;

  public List<String> getInlineCmd() {
    return inlineCommand;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }
}
