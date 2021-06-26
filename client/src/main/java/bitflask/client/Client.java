package bitflask.client;

import bitflask.client.repl.REPL;
import bitflask.resp.Resp;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class Client {

  private static final String TERMINATING_CONNECTION = "Disconnecting server";

  private static final int SERVER_PORT = 9090;

  private final Socket socket;
  private final Resp resp;
  private final REPL repl;

  public Client() throws IOException {
    this.socket = new Socket(InetAddress.getLocalHost(), SERVER_PORT);
    this.resp = new Resp(this.socket);
    this.repl = new REPL(this.resp);
  }

  public void start() {
    repl.start();
    this.close();
  }

  private void close() {
    try {
      socket.close();
      System.out.println(TERMINATING_CONNECTION);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    System.out.println("Hello from client");

    try {
      Client client = new Client();
      client.start();
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }

    System.exit(0);
  }
}