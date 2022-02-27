package dev.sbutler.bitflask.server.client_processing;

import dev.sbutler.bitflask.resp.types.RespBulkString;
import dev.sbutler.bitflask.resp.types.RespType;
import dev.sbutler.bitflask.resp.network.reader.RespReader;
import dev.sbutler.bitflask.resp.network.writer.RespWriter;
import dev.sbutler.bitflask.server.command_processing.CommandProcessor;
import dev.sbutler.bitflask.server.command_processing.ServerCommand;
import java.io.EOFException;
import java.io.IOException;

public class ClientMessageProcessor {

  private final CommandProcessor commandProcessor;
  private final RespReader respReader;
  private final RespWriter respWriter;

  ClientMessageProcessor(CommandProcessor commandProcessor, RespReader respReader,
      RespWriter respWriter) {
    this.commandProcessor = commandProcessor;
    this.respReader = respReader;
    this.respWriter = respWriter;
  }

  public boolean processNextMessage() {
    try {
      RespType<?> clientMessage = readClientMessage();
      RespType<?> response = getServerResponseToClient(clientMessage);
      writeResponseMessage(response);
      return true;
    } catch (EOFException e) {
      System.out.println("Client disconnected.");
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }
    return false;
  }

  private RespType<?> readClientMessage() throws IOException {
    return respReader.readNextRespType();
  }

  private RespType<?> getServerResponseToClient(RespType<?> clientMessage) throws IOException {
    System.out.printf("S: received from client %s%n", clientMessage);

    String response;
    try {
      // todo: differentiate between invalid format and invalid command and terminate connection accordingly
      ServerCommand command = ServerCommand.valueOf(clientMessage);
      response = commandProcessor.processServerCommand(command);
    } catch (IllegalArgumentException e) {
      response = "Invalid command: " + e.getMessage();
    }

    return new RespBulkString(response);
  }

  private void writeResponseMessage(RespType<?> response) throws IOException {
    respWriter.writeRespType(response);
  }

}
