package dev.sbutler.bitflask.client.command_processing;

import dev.sbutler.bitflask.resp.network.RespReader;
import dev.sbutler.bitflask.resp.network.RespWriter;
import java.io.IOException;
import javax.inject.Inject;

/**
 * Handles processing commands via communication with a remote server using the RESP protocol.
 */
public class RespCommandProcessor {

  private final RespReader respReader;
  private final RespWriter respWriter;

  @Inject
  public RespCommandProcessor(RespReader respReader, RespWriter respWriter) {
    this.respReader = respReader;
    this.respWriter = respWriter;
  }

  public String runCommand(RemoteCommand command) throws ProcessingException {
    writeCommand(command);
    return readResponse();
  }

  private void writeCommand(RemoteCommand command) throws ProcessingException {
    try {
      respWriter.writeRespElement(command.getAsRespArray());
    } catch (IOException e) {
      throw new ProcessingException("Failed to write command", e);
    }
  }

  private String readResponse() throws ProcessingException {
    try {
      return respReader.readNextRespElement().toString();
    } catch (IOException e) {
      throw new ProcessingException("Failed to read response", e);
    }
  }
}
