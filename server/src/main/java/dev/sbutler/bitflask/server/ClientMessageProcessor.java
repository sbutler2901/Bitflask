package dev.sbutler.bitflask.server;

import dev.sbutler.bitflask.resp.RespBulkString;
import dev.sbutler.bitflask.resp.RespType;
import dev.sbutler.bitflask.resp.RespUtils;
import dev.sbutler.bitflask.server.processing.CommandProcessor;
import dev.sbutler.bitflask.server.processing.ServerCommand;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;

public class ClientMessageProcessor {

  private final CommandProcessor commandProcessor;
  private final BufferedReader bufferedReader;
  private final BufferedOutputStream bufferedOutputStream;

  ClientMessageProcessor(CommandProcessor commandProcessor, BufferedReader bufferedReader,
      BufferedOutputStream bufferedOutputStream) {
    this.commandProcessor = commandProcessor;
    this.bufferedReader = bufferedReader;
    this.bufferedOutputStream = bufferedOutputStream;
  }

  public boolean processNextMessage() {
    try {
      RespType<?> clientMessage = readClientMessage();
      RespType<?> response = getServerResponseToClient(clientMessage);
      writeResponseMessage(response);
    } catch (EOFException e) {
      System.out.println("Client disconnected.");
      return false;
    } catch (IOException e) {
      System.out.println(e.getMessage());
      return false;
    }
    return true;
  }

  private RespType<?> readClientMessage() throws IOException {
    return RespUtils.readNextRespType(bufferedReader);
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
    bufferedOutputStream.write(response.getEncodedBytes());
    bufferedOutputStream.flush();
  }

}
