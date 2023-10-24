package dev.sbutler.bitflask.server;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.resp.network.RespService;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/** Handles the processing of a specific client including messages and network resources. */
final class RespClientHandlingService extends AbstractService implements Runnable {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ListeningExecutorService listeningExecutorService;
  private final RespClientMessageProcessor respClientMessageProcessor;

  private RespClientHandlingService(
      ListeningExecutorService listeningExecutorService,
      RespClientMessageProcessor respClientMessageProcessor) {
    this.listeningExecutorService = listeningExecutorService;
    this.respClientMessageProcessor = respClientMessageProcessor;
  }

  static class Factory {

    private final ListeningExecutorService listeningExecutorService;
    private final RespClientMessageProcessor.Factory clientMessageProcessorFactory;

    @Inject
    Factory(
        ListeningExecutorService listeningExecutorService,
        RespClientMessageProcessor.Factory clientMessageProcessorFactory) {
      this.listeningExecutorService = listeningExecutorService;
      this.clientMessageProcessorFactory = clientMessageProcessorFactory;
    }

    RespClientHandlingService create(SocketChannel socketChannel) throws IOException {
      RespService respService = RespService.create(socketChannel);
      RespClientMessageProcessor respClientMessageProcessor =
          clientMessageProcessorFactory.create(respService);
      return new RespClientHandlingService(listeningExecutorService, respClientMessageProcessor);
    }
  }

  @Override
  protected void doStart() {
    Futures.submit(this, listeningExecutorService);
  }

  public void run() {
    notifyStarted();
    try {
      while (isRunning()
          && respClientMessageProcessor.isOpen()
          && !Thread.currentThread().isInterrupted()) {
        boolean shouldContinueRunning = respClientMessageProcessor.processNextMessage();
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
      respClientMessageProcessor.close();
      notifyStopped();
    } catch (IOException e) {
      logger.atSevere().withCause(e).log("Failed to close the RespClientMessageProcessor");
      notifyFailed(e);
    } catch (Exception e) {
      logger.atSevere().withCause(e).log("Unexpected error while stopping");
      notifyFailed(e);
    }
  }
}
