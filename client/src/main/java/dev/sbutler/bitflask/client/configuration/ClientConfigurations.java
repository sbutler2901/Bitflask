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
public class ClientConfigurations implements Configurations {

  @Parameter(names = {
      ClientConfigurationsConstants.SERVER_HOST_FLAG_SHORT,
      ClientConfigurationsConstants.SERVER_HOST_FLAG_LONG},
      validateWith = NonBlankStringValidator.class,
      description = "The host running the Bitflask server"
  )
  private String host;

  @Parameter(names = {
      ClientConfigurationsConstants.SERVER_PORT_FLAG_SHORT,
      ClientConfigurationsConstants.SERVER_PORT_FLAG_LONG},
      validateWith = PositiveIntegerValidator.class,
      description = "The port that the Bitflask server is listening on"
  )
  private int port;

  @Parameter(description = "inline command")
  private List<String> inlineCommand = new ArrayList<>();

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public List<String> getInlineCmd() {
    return inlineCommand;
  }

  // Derived configurations

  public boolean getUsePrompt() {
    return inlineCommand.size() == 0;
  }

  @Override
  public String toString() {
    return "ClientConfigurations{" +
        "host='" + host + '\'' +
        ", port=" + port +
        ", inlineCommand=" + inlineCommand +
        '}';
  }
}
