package dev.sbutler.bitflask.server;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.*;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import dev.sbutler.bitflask.resp.network.RespService;
import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

/**
 * Handles accepting incoming RESP based client connections and spawning a {@link
 * RespClientRequestProcessor}.
 *
 * <p>If this service is shutdown, all spawned RespClientRequestProcessors that are still running
 * will also be shutdown.
 */
final class RespNetworkService extends AbstractExecutionThreadService {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ListeningExecutorService listeningExecutorService;
  private final RespClientRequestProcessor.Factory clientRequestProcessorFactory;
  private final ServerSocketChannel serverSocketChannel;

  private final Set<RespClientRequestProcessor> runningRespClientRequestProcessors =
      ConcurrentHashMap.newKeySet();
  private volatile boolean shouldContinueRunning = true;

  @Inject
  RespNetworkService(
      ListeningExecutorService listeningExecutorService,
      RespClientRequestProcessor.Factory clientRequestProcessorFactory,
      @Assisted ServerSocketChannel serverSocketChannel) {
    this.listeningExecutorService = listeningExecutorService;
    this.clientRequestProcessorFactory = clientRequestProcessorFactory;
    this.serverSocketChannel = serverSocketChannel;
  }

  interface Factory {
    RespNetworkService create(ServerSocketChannel serverSocketChannel);
  }

  @Override
  protected void run() {
    try {
      while (shouldContinueRunning
          && serverSocketChannel.isOpen()
          && !Thread.currentThread().isInterrupted()) {
        Optional<RespService> respService = acceptNextRespConnection();
        if (respService.isEmpty()) {
          break;
        }
        createAndStartClientRequestProcessor(respService.get());
      }
    } finally {
      triggerShutdown();
    }
  }

  private Optional<RespService> acceptNextRespConnection() {
    SocketChannel socketChannel;
    try {
      socketChannel = serverSocketChannel.accept();
      logger.atInfo().log(
          "Received incoming client connection from [%s]", socketChannel.getRemoteAddress());
    } catch (IOException e) {
      logger.atSevere().withCause(e).log("Failed to accept resp connection. Shutting down.");
      triggerShutdown();
      return Optional.empty();
    }

    try {
      return Optional.of(RespService.create(socketChannel));
    } catch (IOException e) {
      logger.atSevere().withCause(e).log(
          "Failed to create RespService for incoming client connection. Shutting down.");
      triggerShutdown();
    }
    return Optional.empty();
  }

  private void createAndStartClientRequestProcessor(RespService respService) {
    RespClientRequestProcessor clientRequestProcessor =
        clientRequestProcessorFactory.create(respService);

    ListenableFuture<Void> clientRequestProcessorFuture =
        Futures.submit(clientRequestProcessor, listeningExecutorService);
    runningRespClientRequestProcessors.add(clientRequestProcessor);

    // Clean up after service has reached a terminal state on its own
    Futures.addCallback(
        clientRequestProcessorFuture,
        new FutureCallback<>() {
          @Override
          public void onSuccess(Void result) {
            runningRespClientRequestProcessors.remove(clientRequestProcessor);
          }

          @Override
          public void onFailure(@Nullable Throwable t) {
            runningRespClientRequestProcessors.remove(clientRequestProcessor);
          }
        },
        listeningExecutorService);
  }

  @Override
  protected void triggerShutdown() {
    shouldContinueRunning = false;
    try {
      serverSocketChannel.close();
    } catch (IOException e) {
      logger.atWarning().withCause(e).log("Error closing the ServerSocketChannel");
    }
    runningRespClientRequestProcessors.forEach(RespClientRequestProcessor::triggerShutdown);
  }
}
