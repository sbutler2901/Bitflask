package dev.sbutler.bitflask.server.client_handling_service;

import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.server.client_handling_service.connection.ClientConnectionManagerImpl;
import dev.sbutler.bitflask.server.client_handling_service.processing.ClientMessageProcessorImpl;
import java.io.Closeable;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ClientRequestHandlerImpl implements Runnable, Closeable {

  private static final String TERMINATING_CONNECTION = "Terminating client session.";
  private static final String TERMINATING_CONNECTION_FAILURE = "Failed to correctly terminate the client session";

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ClientConnectionManagerImpl clientConnectionManager;
  private final ClientMessageProcessorImpl clientMessageProcessor;

  private volatile boolean shouldContinueRunning = true;

  @Inject
  ClientRequestHandlerImpl(ClientConnectionManagerImpl clientConnectionManager,
      ClientMessageProcessorImpl clientMessageProcessor) {
    this.clientConnectionManager = clientConnectionManager;
    this.clientMessageProcessor = clientMessageProcessor;
  }

  @Override
  public void run() {
    processClientMessages();
    closeClientConnection();
  }

  private void processClientMessages() {
    while (!Thread.currentThread().isInterrupted() && shouldContinueRunning) {
      shouldContinueRunning = clientMessageProcessor.processNextMessage();
    }
  }

  private void closeClientConnection() {
    logger.atInfo().log(TERMINATING_CONNECTION);
    try {
      close();
    } catch (IOException e) {
      logger.atSevere().withCause(e).log(TERMINATING_CONNECTION_FAILURE);
    }
  }

  @Override
  public void close() throws IOException {
    shouldContinueRunning = false;
    clientConnectionManager.close();
  }

}