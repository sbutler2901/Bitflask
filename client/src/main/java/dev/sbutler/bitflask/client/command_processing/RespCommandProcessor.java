package dev.sbutler.bitflask.client.command_processing;

import dev.sbutler.bitflask.resp.network.RespService;
import java.io.IOException;
import javax.inject.Inject;

/**
 * Handles processing commands via communication with a remote server using the RESP protocol.
 */
public class RespCommandProcessor {

  private final RespService respService;

  @Inject
  public RespCommandProcessor(RespService respService) {
    this.respService = respService;
  }

  public String runCommand(RemoteCommand command) throws ProcessingException {
    writeCommand(command);
    return readResponse();
  }

  private void writeCommand(RemoteCommand command) throws ProcessingException {
    try {
      respService.write(command.getAsRespArray());
    } catch (IOException e) {
      throw new ProcessingException("Failed to write command", e);
    }
  }

  private String readResponse() throws ProcessingException {
    try {
      return respService.read().toString();
    } catch (IOException e) {
      throw new ProcessingException("Failed to read response", e);
    }
  }
}
