package dev.sbutler.bitflask.client.configuration;

import com.beust.jcommander.Parameter;
import dev.sbutler.bitflask.common.configuration.validators.NonBlankStringValidator;
import dev.sbutler.bitflask.common.configuration.validators.PositiveIntegerValidator;

public class ClientConfiguration {

  @Parameter(description = "inline command")
  private String inlineCommand = "";

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

  public String getInlineCmd() {
    return inlineCommand;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }
}
