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

  private boolean shouldContinueRunning = true;

  @Inject
  ClientRequestHandlerImpl(ClientConnectionManager clientConnectionManager,
      ClientMessageProcessor clientMessageProcessor) {
    this.clientConnectionManager = clientConnectionManager;
    this.clientMessageProcessor = clientMessageProcessor;
  }

  @Override
  public void run() {
    while (!Thread.currentThread().isInterrupted() && shouldContinueRunning) {
      shouldContinueRunning = clientMessageProcessor.processNextMessage();
    }
    logger.debug("Closing. isInterrupted {}", Thread.currentThread().isInterrupted());
    close();
  }

  public void close() {
    try {
      shouldContinueRunning = false;
      clientConnectionManager.close();
      logger.info(TERMINATING_CONNECTION);
    } catch (IOException e) {
      logger.error(TERMINATING_CONNECTION_FAILURE, e);
    }
  }
}
