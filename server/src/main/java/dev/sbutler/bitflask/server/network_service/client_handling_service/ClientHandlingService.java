package dev.sbutler.bitflask.server.network_service.client_handling_service;

import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.resp.network.RespService;
import java.io.Closeable;
import java.io.IOException;

/**
 * Handles the processing of a specific client including messages and network resources.
 */
public class ClientHandlingService implements Runnable, Closeable {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final RespService respService;
  private final ClientMessageProcessor clientMessageProcessor;

  private volatile boolean shouldContinueRunning = true;

  private ClientHandlingService(RespService respService,
      ClientMessageProcessor clientMessageProcessor) {
    this.respService = respService;
    this.clientMessageProcessor = clientMessageProcessor;
  }

  public static ClientHandlingService create(RespService respService,
      ClientMessageProcessor clientMessageProcessor) {
    return new ClientHandlingService(respService, clientMessageProcessor);
  }

  @Override
  public void run() {
    try {
      processClientMessages();
    } catch (Exception e) {
      logger.atSevere().withCause(e)
          .log("Processing client messages failed. Terminating connection!");
    } finally {
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
      respService.close();
    } catch (IOException e) {
      logger.atSevere().withCause(e).log("Failed to correctly terminate the client session");
    }
  }
}
