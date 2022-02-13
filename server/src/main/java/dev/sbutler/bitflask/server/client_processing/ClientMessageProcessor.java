package dev.sbutler.bitflask.server.client_processing;

import dev.sbutler.bitflask.resp.types.RespBulkString;
import dev.sbutler.bitflask.resp.types.RespType;
import dev.sbutler.bitflask.resp.utilities.RespReader;
import dev.sbutler.bitflask.resp.utilities.RespWriter;
import dev.sbutler.bitflask.server.command_processing.CommandProcessor;
import dev.sbutler.bitflask.server.command_processing.ServerCommand;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class ClientMessageProcessor {

  private final CommandProcessor commandProcessor;
  private final RespReader respReader;
  private final RespWriter respWriter;

  ClientMessageProcessor(CommandProcessor commandProcessor, InputStream inputStream,
      OutputStream outputStream) {
    this.commandProcessor = commandProcessor;
    this.respReader = new RespReader(new BufferedReader(new InputStreamReader(inputStream)));
    this.respWriter = new RespWriter(new BufferedOutputStream(outputStream));
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
