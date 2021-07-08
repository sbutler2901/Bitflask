package bitflask.server;

import bitflask.resp.RespBulkString;
import bitflask.resp.RespType;
import bitflask.resp.RespUtils;
import bitflask.server.storage.Storage;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
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

  private RespType<?> processRequest(RespType<?> clientMessage) throws IOException {
    ServerCommand command;
    try {
      command = new ServerCommand(clientMessage);
    } catch (IllegalArgumentException e) {
      return new RespBulkString("Invalid message: " + e.getMessage());
    }

    String key, value;
    switch(command.getCommand()) {
      case GET:
        key = command.getArgs().get(0);
        value = storage.read(key).orElse("Not Found");
        return new RespBulkString(value);
      case SET:
        key = command.getArgs().get(0);
        value = command.getArgs().get(1);
        storage.write(key, value);
        return new RespBulkString("Ok");
      case PING:
        return new RespBulkString("Pong");
      default:
        return new RespBulkString("Server issue processing command");
    }
  }

  @Override
  public void run() {
    while (!Thread.interrupted() && isRunning) {
      try {
        RespType<?> clientMessage = RespUtils.readNextRespType(bufferedReader);
        System.out.printf("S: received from client %s%n", clientMessage);

        RespType<?> response = processRequest(clientMessage);

        response.write(bufferedOutputStream);
        bufferedOutputStream.flush();
      } catch (IOException e) {
        System.out.println("Client disconnected. Closing thread");
        this.close();
      }
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
