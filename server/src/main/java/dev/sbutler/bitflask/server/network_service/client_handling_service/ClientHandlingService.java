package dev.sbutler.bitflask.server.network_service.client_handling_service;

import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.server.network_service.client_handling_service.connection.ClientConnectionManager;
import dev.sbutler.bitflask.server.network_service.client_handling_service.processing.ClientMessageProcessor;
import java.io.Closeable;
import java.io.IOException;
import javax.inject.Inject;

public class ClientHandlingService implements Runnable, Closeable {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ClientConnectionManager clientConnectionManager;
  private final ClientMessageProcessor clientMessageProcessor;

  private volatile boolean shouldContinueRunning = true;

  @Inject
  ClientHandlingService(ClientConnectionManager clientConnectionManager,
      ClientMessageProcessor clientMessageProcessor) {
    this.clientConnectionManager = clientConnectionManager;
    this.clientMessageProcessor = clientMessageProcessor;
  }

  @Override
  public void run() {
    try {
      processClientMessages();
      if (shouldContinueRunning) {
        close();
      }
    } catch (Exception e) {
      logger.atSevere().withCause(e)
          .log("Processing client messages failed. Terminating connection!");
      close();
    }
  }

  private void processClientMessages() {
    while (!Thread.currentThread().isInterrupted() && shouldContinueRunning) {
      shouldContinueRunning = clientMessageProcessor.processNextMessage();
    }
  }

  @Override
  public void close() {
    logger.atInfo().log("Terminating client session.");
    shouldContinueRunning = false;
    try {
      clientConnectionManager.close();
    } catch (IOException e) {
      logger.atSevere().withCause(e).log("Failed to correctly terminate the client session");
    }
  }

}
