package dev.sbutler.bitflask.client;

import com.sun.jdi.InternalException;
import dev.sbutler.bitflask.client.command_processing.CommandProcessor;
import dev.sbutler.bitflask.client.repl.Repl;
import dev.sbutler.bitflask.client.repl.input.StdinInputParser;
import dev.sbutler.bitflask.client.repl.output.StdoutOutputWriter;
import dev.sbutler.bitflask.resp.utilities.RespReader;
import dev.sbutler.bitflask.resp.utilities.RespWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class Client {

  private static final String INITIALIZATION_FAILURE = "Failed to initialize the client";
  private static final String TERMINATING_CONNECTION = "Disconnecting server";
  private static final String TERMINATION_FAILURE = "Failed to close the socket";

  private static final int SERVER_PORT = 9090;

  private final Socket socket;
  private final CommandProcessor commandProcessor;

  public Client(Socket socket, CommandProcessor commandProcessor) {
    this.socket = socket;
    this.commandProcessor = commandProcessor;
  }

  public static void main(String[] args) {
    Client client = initializeClient();
    client.start();
    client.close();
  }

  private static Client initializeClient() {
    try {
      Socket socket = new Socket(InetAddress.getLocalHost(), SERVER_PORT);
      CommandProcessor commandProcessor = new CommandProcessor(
          new RespReader(socket.getInputStream()),
          new RespWriter(socket.getOutputStream())
      );
      return new Client(socket, commandProcessor);
    } catch (IOException e) {
      throw new InternalException(INITIALIZATION_FAILURE);
    }
  }

  public void start() {
    Repl repl = new Repl(
        commandProcessor,
        new StdinInputParser(),
        new StdoutOutputWriter()
    );
    repl.start();
  }

  public void close() {
    System.out.println(TERMINATING_CONNECTION);
    try {
      socket.close();
    } catch (IOException e) {
      throw new InternalException(TERMINATION_FAILURE);
    }
  }
}