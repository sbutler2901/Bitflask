package dev.sbutler.bitflask.client;

import dev.sbutler.bitflask.client.repl.REPL;
import dev.sbutler.bitflask.resp.utilities.RespReader;
import dev.sbutler.bitflask.resp.utilities.RespWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import lombok.Getter;

public class Client {

  private static final String TERMINATING_CONNECTION = "Disconnecting server";

  private static final int SERVER_PORT = 9090;

  private final Socket socket;
  private final RespReader respReader;
  private final RespWriter respWriter;

  @Getter
  private final String serverAddress;

  public Client() throws IOException {
    this.socket = new Socket(InetAddress.getLocalHost(), SERVER_PORT);
    this.respReader = new RespReader(socket.getInputStream());
    this.respWriter = new RespWriter(socket.getOutputStream());
    this.serverAddress = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
  }

  public static void main(String[] args) {
    System.out.println("Hello from client");

    try {
      Client client = new Client();
      REPL repl = new REPL(client);

      repl.start();

      client.close();
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }

    System.exit(0);
  }

  public String runCommand(ClientCommand command) throws IOException {
    respWriter.writeRespType(command.getCommandRespArray());
    return respReader.readNextRespType().toString();
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