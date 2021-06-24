package bitflask.client;

import bitflask.client.repl.REPL;
import bitflask.resp.RespArray;
import bitflask.resp.RespType;
import bitflask.resp.RespUtils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import org.apache.commons.io.IOUtils;

public class Client {

  private static final String TERMINATING_CONNECTION = "Disconnecting server";

  private static final int SERVER_PORT = 9090;
  private static final long SLEEP_INTERVAL_SERVER_INPUT = 1000; // 1 second


  private final Socket serverSocket;
  BufferedInputStream bufferedInputStream;
  BufferedOutputStream bufferedOutputStream;
  private boolean isRunning = true;

  public Client(Socket serverSocket) throws IOException {
    this.serverSocket = serverSocket;

    this.bufferedInputStream = IOUtils.buffer(serverSocket.getInputStream());
    this.bufferedOutputStream = IOUtils.buffer(serverSocket.getOutputStream());
  }

  public static void main(String[] args) {
    System.out.println("Hello from client");

    try {
      Socket socket = new Socket(InetAddress.getLocalHost(), SERVER_PORT);
      Client client = new Client(socket);
      REPL repl = new REPL(client);

      repl.start();

      client.close();
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }

    System.exit(0);
  }

  private void sendToServer(byte[] message) throws IOException {
    IOUtils.write(message, bufferedOutputStream);
    bufferedOutputStream.flush();
  }

  private byte[] receiveFromServer() throws IOException {
    try {
      while (!Thread.interrupted() && isRunning) {
        if (bufferedInputStream.available() <= 0) {
          Thread.sleep(SLEEP_INTERVAL_SERVER_INPUT);
        } else {
          break;
        }
      }

      int numAvailableBytes = bufferedInputStream.available();
      byte[] readBytes = new byte[numAvailableBytes];
      IOUtils.read(bufferedInputStream, readBytes);

      return readBytes;
    } catch (InterruptedException e) {
      System.out.println("C: thread interrupted. Returning without reading");
    }

    return null;
  }

  public void sendCommand(RespArray commandArray) {
    try {

      sendToServer(commandArray.getEncodedBytes());
      byte[] serverResponse = receiveFromServer();

      if (serverResponse == null || serverResponse.length == 0) {
        System.out.println("C: empty response from server");
        return;
      }

      RespType<?> respType = RespUtils.from(serverResponse);
      System.out.println("S: " + respType.getDecodedValue());


    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void close() {
    try {
      isRunning = false;
      serverSocket.close();
      System.out.println(TERMINATING_CONNECTION);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}