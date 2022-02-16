package dev.sbutler.bitflask.client;

import dev.sbutler.bitflask.client.repl.REPL;
import dev.sbutler.bitflask.resp.utilities.RespReader;
import dev.sbutler.bitflask.resp.utilities.RespWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class Client {

  private static final String TERMINATING_CONNECTION = "Disconnecting server";

  private static final int SERVER_PORT = 9090;

  private final Socket socket;
  private final CommandProcessor commandProcessor;

  public Client() throws IOException {
    this.socket = new Socket(InetAddress.getLocalHost(), SERVER_PORT);
    this.commandProcessor = new CommandProcessor(
        new RespReader(socket.getInputStream()),
        new RespWriter(socket.getOutputStream())
    );
  }

  public static void main(String[] args) {
    System.out.println("Hello from client");

    try {
      Client client = new Client();
      client.runWithRepl();
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }

    System.exit(0);
  }

  private void runWithRepl() {
    REPL repl = new REPL(commandProcessor);
    repl.start();
    close();
  }

  private void close() {
    try {
      socket.close();
      System.out.println(TERMINATING_CONNECTION);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}