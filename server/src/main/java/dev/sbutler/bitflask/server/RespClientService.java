package dev.sbutler.bitflask.server;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.resp.network.RespService;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/** Manages the lifecycle of a single RESP based client's connection. */
final class RespClientService extends AbstractService implements Runnable {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ListeningExecutorService listeningExecutorService;
  private final RespClientRequestProcessor respClientRequestProcessor;

  private RespClientService(
      ListeningExecutorService listeningExecutorService,
      RespClientRequestProcessor respClientRequestProcessor) {
    this.listeningExecutorService = listeningExecutorService;
    this.respClientRequestProcessor = respClientRequestProcessor;
  }

  static class Factory {

    private final ListeningExecutorService listeningExecutorService;
    private final RespClientRequestProcessor.Factory clientMessageProcessorFactory;

    @Inject
    Factory(
        ListeningExecutorService listeningExecutorService,
        RespClientRequestProcessor.Factory clientMessageProcessorFactory) {
      this.listeningExecutorService = listeningExecutorService;
      this.clientMessageProcessorFactory = clientMessageProcessorFactory;
    }

    RespClientService create(SocketChannel socketChannel) throws IOException {
      RespService respService = RespService.create(socketChannel);
      RespClientRequestProcessor respClientRequestProcessor =
          clientMessageProcessorFactory.create(respService);
      return new RespClientService(listeningExecutorService, respClientRequestProcessor);
    }
  }

  @Override
  protected void doStart() {
    // Bootstrap service
    Futures.submit(this, listeningExecutorService);
  }

  @Override
  public void run() {
    notifyStarted();
    try {
      while (isRunning()
          && respClientRequestProcessor.isOpen()
          && !Thread.currentThread().isInterrupted()) {
        boolean shouldContinueRunning = respClientRequestProcessor.processNextMessage();
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
      respClientRequestProcessor.close();
      notifyStopped();
    } catch (IOException e) {
      logger.atSevere().withCause(e).log("Failed to close the RespClientRequestProcessor");
      notifyFailed(e);
    } catch (Exception e) {
      logger.atSevere().withCause(e).log("Unexpected error while stopping");
      notifyFailed(e);
    }
  }
}
