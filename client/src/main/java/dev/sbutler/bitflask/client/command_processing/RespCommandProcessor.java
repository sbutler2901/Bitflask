package dev.sbutler.bitflask.client.command_processing;

import dev.sbutler.bitflask.client.network.RespServiceProvider;
import java.io.IOException;
import javax.inject.Inject;

/**
 * Handles processing commands via communication with a remote server using the RESP protocol.
 */
public class RespCommandProcessor {

  private final RespServiceProvider respServiceProvider;

  @Inject
  public RespCommandProcessor(RespServiceProvider respServiceProvider) {
    this.respServiceProvider = respServiceProvider;
  }

  public String runCommand(RemoteCommand command) throws ProcessingException {
    writeCommand(command);
    return readResponse();
  }

  private void writeCommand(RemoteCommand command) throws ProcessingException {
    try {
      respServiceProvider.get().write(command.getAsRespArray());
    } catch (IOException e) {
      throw new ProcessingException("Failed to write command", e);
    }
  }

  private String readResponse() throws ProcessingException {
    try {
      return respServiceProvider.get().read().toString();
    } catch (IOException e) {
      throw new ProcessingException("Failed to read response", e);
    }
  }
}
