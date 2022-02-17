package dev.sbutler.bitflask.client.command_processing;

import dev.sbutler.bitflask.resp.utilities.RespReader;
import dev.sbutler.bitflask.resp.utilities.RespWriter;
import java.io.IOException;

public class CommandProcessor {

  private final RespReader respReader;
  private final RespWriter respWriter;

  public CommandProcessor(RespReader respReader, RespWriter respWriter) {
    this.respReader = respReader;
    this.respWriter = respWriter;
  }

  public String runCommand(ClientCommand command) throws IOException {
    respWriter.writeRespType(command.getAsRespArray());
    return respReader.readNextRespType().toString();
  }
}
