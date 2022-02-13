package dev.sbutler.bitflask.server;

import dev.sbutler.bitflask.server.processing.CommandProcessor;
import dev.sbutler.bitflask.server.storage.Storage;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class RequestHandler implements Runnable {

  private static final String TERMINATING_CONNECTION = "Terminating session.";

  private final Socket socket;
  private final RequestProcessor requestProcessor;

  public RequestHandler(Socket socket, Storage storage) throws IOException {
    this.socket = socket;
    this.requestProcessor = new RequestProcessor(
        new CommandProcessor(storage),
        new BufferedReader(new InputStreamReader(socket.getInputStream())),
        new BufferedOutputStream(socket.getOutputStream())
    );
  }

  @Override
  public void run() {
    while (!Thread.interrupted()) {
      boolean processedSuccessfully = requestProcessor.processRequest();
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
