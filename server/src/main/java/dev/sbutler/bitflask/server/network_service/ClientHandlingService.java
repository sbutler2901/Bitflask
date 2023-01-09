package dev.sbutler.bitflask.server.network_service;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.resp.network.RespService;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import javax.inject.Inject;

/**
 * Handles the processing of a specific client including messages and network resources.
 */
final class ClientHandlingService extends AbstractService implements Runnable {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  static class Factory {

    private final ListeningExecutorService listeningExecutorService;
    private final ClientMessageProcessor.Factory clientMessageProcessorFactory;

    @Inject
    Factory(ListeningExecutorService listeningExecutorService,
        ClientMessageProcessor.Factory clientMessageProcessorFactory) {
      this.listeningExecutorService = listeningExecutorService;
      this.clientMessageProcessorFactory = clientMessageProcessorFactory;
    }

    ClientHandlingService create(SocketChannel socketChannel) throws IOException {
      RespService respService = RespService.create(socketChannel);
      ClientMessageProcessor clientMessageProcessor =
          clientMessageProcessorFactory.create(respService);
      return new ClientHandlingService(listeningExecutorService, clientMessageProcessor);
    }
  }

  private final ListeningExecutorService listeningExecutorService;
  private final ClientMessageProcessor clientMessageProcessor;

  ClientHandlingService(ListeningExecutorService listeningExecutorService,
      ClientMessageProcessor clientMessageProcessor) {
    this.listeningExecutorService = listeningExecutorService;
    this.clientMessageProcessor = clientMessageProcessor;
  }

  @Override
  protected void doStart() {
    Futures.submit(this, listeningExecutorService);
  }

  public void run() {
    notifyStarted();
    try {
      while (isRunning() && clientMessageProcessor.isOpen()
          && !Thread.currentThread().isInterrupted()) {
        boolean shouldContinueRunning = clientMessageProcessor.processNextMessage();
        if (!shouldContinueRunning) {
          break;
        }
      }
    } finally {
      // Ensure resources are closed
      stopAsync();
    }
  }

  @Override
  protected void doStop() {
    try {
      clientMessageProcessor.close();
      notifyStopped();
    } catch (IOException e) {
      logger.atSevere().withCause(e).log("Failed to close the ClientMessageProcessor");
      notifyFailed(e);
    } catch (Exception e) {
      logger.atSevere().withCause(e).log("Unexpected error while stopping");
      notifyFailed(e);
    }
  }
}
