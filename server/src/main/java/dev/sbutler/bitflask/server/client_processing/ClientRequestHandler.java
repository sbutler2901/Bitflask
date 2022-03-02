package dev.sbutler.bitflask.server.client_processing;

import com.google.inject.Inject;
import dev.sbutler.bitflask.server.client_connection.ClientConnectionManager;
import java.io.IOException;

public class ClientRequestHandler implements Runnable {

  private static final String TERMINATING_CONNECTION = "Terminating session.";

  private final ClientConnectionManager clientConnectionManager;
  private final ClientMessageProcessor clientMessageProcessor;

  private boolean shouldContinueRunning = true;

  @Inject
  public ClientRequestHandler(ClientConnectionManager clientConnectionManager,
      ClientMessageProcessor clientMessageProcessor) {
    this.clientConnectionManager = clientConnectionManager;
    this.clientMessageProcessor = clientMessageProcessor;
  }

  @Override
  public void run() {
    while (!Thread.interrupted() && shouldContinueRunning) {
      shouldContinueRunning = clientMessageProcessor.processNextMessage();
    }
    close();
  }

  public void close() {
    try {
      shouldContinueRunning = false;
      clientConnectionManager.close();
      System.out.println(TERMINATING_CONNECTION);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
