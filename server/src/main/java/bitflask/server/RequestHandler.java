package bitflask.server;

import bitflask.resp.RespBulkString;
import bitflask.resp.RespType;
import bitflask.resp.RespUtils;
import bitflask.server.storage.Storage;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class RequestHandler implements Runnable {

  private static final String TERMINATING_CONNECTION = "Disconnecting client";

  private final Storage storage;
  private final Socket socket;
  private final BufferedOutputStream bufferedOutputStream;
  private final BufferedReader bufferedReader;
  private boolean isRunning = true;

  public RequestHandler(Socket socket, Storage storage) throws IOException {
    this.socket = socket;
    this.storage = storage;
    this.bufferedOutputStream = new BufferedOutputStream(socket.getOutputStream());
    this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
  }

  private RespType<?> processServerCommand(ServerCommand serverCommand) throws IOException {
    String key, value;
    switch (serverCommand.getCommand()) {
      case GET:
        key = serverCommand.getArgs().get(0);
        value = storage.read(key).orElse("Not Found");
        return new RespBulkString(value);
      case SET:
        key = serverCommand.getArgs().get(0);
        value = serverCommand.getArgs().get(1);
        storage.write(key, value);
        return new RespBulkString("Ok");
      case PING:
        return new RespBulkString("Pong");
      default:
        return new RespBulkString("Server issue processing command");
    }
  }

  private RespType<?> getServerResponse(RespType<?> clientMessage) throws IOException {
    try {
      ServerCommand command = new ServerCommand(clientMessage);
      return processServerCommand(command);
    } catch (IllegalArgumentException e) {
      return new RespBulkString("Invalid message: " + e.getMessage());
    }
  }

  private RespType<?> readClientMessage() throws IOException {
    RespType<?> clientMessage = RespUtils.readNextRespType(bufferedReader);
    System.out.printf("S: received from client %s%n", clientMessage);
    return clientMessage;
  }

  private void writeResponseMessage(RespType<?> response) throws IOException {
    bufferedOutputStream.write(response.getEncodedBytes());
    bufferedOutputStream.flush();
  }

  private void processRequest() {
    try {
      RespType<?> clientMessage = readClientMessage();
      RespType<?> response = getServerResponse(clientMessage);
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
