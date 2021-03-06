package dev.sbutler.bitflask.client.command_processing;

import dev.sbutler.bitflask.resp.network.reader.RespReader;
import dev.sbutler.bitflask.resp.network.writer.RespWriter;
import java.io.IOException;
import javax.inject.Inject;

public class RespCommandProcessor implements CommandProcessor {

  private final RespReader respReader;
  private final RespWriter respWriter;

  @Inject
  public RespCommandProcessor(RespReader respReader, RespWriter respWriter) {
    this.respReader = respReader;
    this.respWriter = respWriter;
  }

  public String runCommand(ClientCommand command) throws ProcessingException {
    writeCommand(command);
    return readResponse();
  }

  private void writeCommand(ClientCommand command) throws ProcessingException {
    try {
      respWriter.writeRespType(command.getAsRespArray());
    } catch (IOException e) {
      throw new ProcessingException("Failed to write command", e);
    }
  }

  private String readResponse() throws ProcessingException {
    try {
      return respReader.readNextRespType().toString();
    } catch (IOException e) {
      throw new ProcessingException("Failed to read response", e);
    }
  }
}
