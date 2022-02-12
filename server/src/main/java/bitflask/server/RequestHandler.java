package bitflask.server;

import bitflask.resp.RespBulkString;
import bitflask.resp.RespType;
import bitflask.resp.RespUtils;
import bitflask.server.processing.CommandProcessor;
import bitflask.server.processing.ServerCommand;
import bitflask.server.storage.Storage;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class RequestHandler implements Runnable {

  private static final String TERMINATING_CONNECTION = "Disconnecting client";

  private final Socket socket;
  private final BufferedOutputStream bufferedOutputStream;
  private final BufferedReader bufferedReader;
  private final CommandProcessor commandProcessor;

  private boolean isRunning = true;

  public RequestHandler(Socket socket, Storage storage) throws IOException {
    this.socket = socket;
    this.bufferedOutputStream = new BufferedOutputStream(socket.getOutputStream());
    this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    this.commandProcessor = new CommandProcessor(storage);
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

  private void processRequest() {
    try {
      RespType<?> clientMessage = readClientMessage();
      RespType<?> response = getServerResponseToClient(clientMessage);
      writeResponseMessage(response);
    } catch (EOFException e) {
      System.out.println("Client disconnected. Closing thread");
      this.close();
    } catch (IOException e) {
      System.out.println(e.getMessage());
      this.close();
    }
  }

  @Override
  public void run() {
    while (!Thread.interrupted() && isRunning) {
      processRequest();
    }
  }

  public void close() {
    try {
      isRunning = false;
      socket.close();
      System.out.println(TERMINATING_CONNECTION);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
