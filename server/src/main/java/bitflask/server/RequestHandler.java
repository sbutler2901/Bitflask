package bitflask.server;

import bitflask.resp.RespArray;
import bitflask.resp.RespBulkString;
import bitflask.resp.RespError;
import bitflask.resp.RespInteger;
import bitflask.resp.RespSimpleString;
import bitflask.resp.RespType;
import bitflask.resp.RespUtils;
import bitflask.server.storage.Storage;
import bitflask.utilities.Command;
import bitflask.utilities.Commands;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Optional;
import org.apache.commons.io.IOUtils;

public class RequestHandler implements Runnable {

  private static final String TERMINATING_CONNECTION = "Disconnecting client";

  private static final long SLEEP_INTERVAL_CLIENT_INPUT = 1000; // 1 second

  private final Socket clientSocket;
  private final Storage storage;
  private final BufferedInputStream bufferedInputStream;
  private final BufferedOutputStream bufferedOutputStream;
  private boolean isRunning = true;

  public RequestHandler(Socket clientSocket, Storage storage) throws IOException {
    this.clientSocket = clientSocket;
    this.storage = storage;

    this.bufferedInputStream = IOUtils.buffer(clientSocket.getInputStream());
    this.bufferedOutputStream = IOUtils.buffer(clientSocket.getOutputStream());
  }

  private void sendToClient(byte[] message) throws IOException {
    IOUtils.write(message, bufferedOutputStream);
    bufferedOutputStream.flush();
  }

  @Override
  public void run() {
    while (!Thread.interrupted() && isRunning) {
      // todo: detect client disconnect
      try {
        if (bufferedInputStream.available() <= 0) {
          try {
            Thread.sleep(SLEEP_INTERVAL_CLIENT_INPUT);
            continue;
          } catch (InterruptedException e) {
            close();
            return;
          }
        }

        int numAvailableBytes = bufferedInputStream.available();
        byte[] readBytes = new byte[numAvailableBytes];
        IOUtils.read(bufferedInputStream, readBytes);

        RespType<?> clientMessageRaw = RespUtils.from(readBytes);
        System.out.printf("S: received from client %s%n", clientMessageRaw);

        RespType<?> response;
        if (clientMessageRaw.getClass() != RespArray.class) {
          response = new RespSimpleString("Incorrect message format. RespArray expected.");
        } else {
          RespArray clientMessage = (RespArray) clientMessageRaw;
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

        sendToClient(response.getEncodedBytes());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void close() {
    try {
      isRunning = false;
      clientSocket.close();
      System.out.println(TERMINATING_CONNECTION);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
