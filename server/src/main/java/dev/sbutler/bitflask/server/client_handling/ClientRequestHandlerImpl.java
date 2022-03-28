package dev.sbutler.bitflask.server.client_handling;

import dev.sbutler.bitflask.server.client_handling.connection.ClientConnectionManager;
import dev.sbutler.bitflask.server.client_handling.processing.ClientMessageProcessor;
import dev.sbutler.bitflask.server.configuration.logging.InjectLogger;
import java.io.IOException;
import javax.inject.Inject;
import org.slf4j.Logger;

class ClientRequestHandlerImpl implements ClientRequestHandler {

  private static final String TERMINATING_CONNECTION = "Terminating client session.";
  private static final String TERMINATING_CONNECTION_FAILURE = "Failed to correctly terminate the client session";

  @InjectLogger
  Logger logger;

  private final ClientConnectionManager clientConnectionManager;
  private final ClientMessageProcessor clientMessageProcessor;

  private volatile boolean shouldContinueRunning = true;

  @Inject
  ClientRequestHandlerImpl(ClientConnectionManager clientConnectionManager,
      ClientMessageProcessor clientMessageProcessor) {
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
    logger.info(TERMINATING_CONNECTION);
    try {
      close();
    } catch (IOException e) {
      logger.error(TERMINATING_CONNECTION_FAILURE, e);
    }
  }

  @Override
  public void close() throws IOException {
    shouldContinueRunning = false;
    clientConnectionManager.close();
  }

}
