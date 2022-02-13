package dev.sbutler.bitflask.server.client_processing;

import dev.sbutler.bitflask.server.command_processing.CommandProcessor;
import dev.sbutler.bitflask.server.storage.Storage;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ClientRequestHandler implements Runnable {

  private static final String TERMINATING_CONNECTION = "Terminating session.";

  private final Socket socket;
  private final ClientMessageProcessor clientMessageProcessor;

  public ClientRequestHandler(Socket socket, Storage storage) throws IOException {
    this.socket = socket;
    this.clientMessageProcessor = new ClientMessageProcessor(
        new CommandProcessor(storage),
        new BufferedReader(new InputStreamReader(socket.getInputStream())),
        new BufferedOutputStream(socket.getOutputStream())
    );
  }

  @Override
  public void run() {
    while (!Thread.interrupted()) {
      boolean processedSuccessfully = clientMessageProcessor.processNextMessage();
      if (!processedSuccessfully) {
        break;
      }
    }
    this.close();
  }

  public void close() {
    try {
      socket.close();
      System.out.println(TERMINATING_CONNECTION);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
