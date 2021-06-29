package bitflask.server;

import bitflask.resp.RespUtils;
import bitflask.resp.RespArray;
import bitflask.resp.RespBulkString;
import bitflask.resp.RespType;
import bitflask.server.storage.Storage;
import bitflask.utilities.Command;
import bitflask.utilities.Commands;
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

  private boolean isValidRequest(RespType respType) {
    if (respType.getClass() != RespArray.class) {
      return false;
    }

    RespArray clientMessage = (RespArray) respType;
    for (RespType clientArg : clientMessage.getValue()) {
      if (clientArg.getClass() != RespBulkString.class) {
        return false;
      }
    }

    return true;
  }

  @Override
  public void run() {
    while (!Thread.interrupted() && isRunning) {
      try {
        RespType clientMessage = RespUtils.readNextRespType(bufferedReader);

        System.out.printf("S: received from client %s%n", clientMessage);

        RespType response;
        if (!isValidRequest(clientMessage)) {
          response = new RespBulkString("Incorrect message format.");
        } else {
          Command clientCommand = new Command((RespArray) clientMessage);

          if (clientCommand.getCommand().equals(Commands.GET)) {
            String key = clientCommand.getArgs().get(0);
            String value = storage.read(key).orElse("Not Found");
            response = new RespBulkString(value);
          } else if (clientCommand.getCommand().equals(Commands.SET)) {
            String key = clientCommand.getArgs().get(0);
            String value = clientCommand.getArgs().get(1);
            storage.write(key, value);
            response = new RespBulkString("Ok");
          } else {
            System.out.println("S: Unsupported received: " + clientCommand.getCommand());
            response = new RespBulkString("Unsupported Command: " + clientCommand.getCommand());
          }
        }

        response.write(bufferedOutputStream);
        bufferedOutputStream.flush();
      } catch (IOException e) {
        this.close();
        e.printStackTrace();
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
