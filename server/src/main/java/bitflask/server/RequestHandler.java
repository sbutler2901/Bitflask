package bitflask.server;

import bitflask.resp.Resp;
import bitflask.resp.RespArray;
import bitflask.resp.RespBulkString;
import bitflask.resp.RespType;
import bitflask.server.storage.Storage;
import bitflask.utilities.Command;
import bitflask.utilities.Commands;
import java.io.IOException;
import java.net.Socket;

public class RequestHandler implements Runnable {

  private static final String TERMINATING_CONNECTION = "Disconnecting client";

  private final Socket socket;
  private final Resp resp;
  private final Storage storage;
  private boolean isRunning = true;

  public RequestHandler(Socket socket, Storage storage) throws IOException {
    this.socket = socket;
    this.storage = storage;
    this.resp = new Resp(this.socket);
  }

  @Override
  public void run() {
    while (!Thread.interrupted() && isRunning) {
      try {
        RespType respType = this.resp.receive();

        if (respType == null) {
          System.out.println("S: Client disconnected");
          this.close();
          break;
        }

        System.out.printf("S: received from client %s%n", respType);

        RespType response;
        if (respType.getClass() != RespArray.class) {
          response = new RespBulkString("Incorrect message format. RespArray expected.");
        } else {
          RespArray clientMessage = (RespArray) respType;
          Command clientCommand = new Command(clientMessage);

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
            System.out.println("S: unknown command: " + clientCommand.getCommand());
            response = new RespBulkString("Unsupported Command: " + clientCommand.getCommand());
          }
        }

        this.resp.send(response);
      } catch (IOException e) {
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
