package dev.sbutler.bitflask.server;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.*;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import dev.sbutler.bitflask.resp.network.RespService;
import java.io.IOException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

  private final Set<ListenableFuture<Void>> respClientRequestProcessorFutures =
      ConcurrentHashMap.newKeySet();

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
      while (isRunning()
          && serverSocketChannel.isOpen()
          && !Thread.currentThread().isInterrupted()) {
        SocketChannel socketChannel = serverSocketChannel.accept();
        logger.atInfo().log(
            "Received incoming client connection from [%s]", socketChannel.getRemoteAddress());

        createAndStartClientRequestProcessor(socketChannel);
      }
    } catch (AsynchronousCloseException ignored) {
      // Service shutdown externally
    } catch (IOException e) {
      logger.atSevere().withCause(e).log(
          "Failed to create RespService for incoming client connection. Shutting down.");
    } finally {
      triggerShutdown();
    }
  }

  private void createAndStartClientRequestProcessor(SocketChannel socketChannel)
      throws IOException {
    RespService respService = RespService.create(socketChannel);
    RespClientRequestProcessor clientRequestProcessor =
        clientRequestProcessorFactory.create(respService);

    ListenableFuture<Void> clientRequestProcessorFuture =
        Futures.submit(clientRequestProcessor, listeningExecutorService);
    respClientRequestProcessorFutures.add(clientRequestProcessorFuture);

    // Clean up after service has reached a terminal state on its own
    Futures.addCallback(
        clientRequestProcessorFuture,
        new FutureCallback<>() {
          @Override
          public void onSuccess(Void result) {
            respClientRequestProcessorFutures.remove(clientRequestProcessorFuture);
          }

          @Override
          public void onFailure(Throwable t) {
            respClientRequestProcessorFutures.remove(clientRequestProcessorFuture);
          }
        },
        listeningExecutorService);
  }

  @Override
  protected void triggerShutdown() {
    close();
    stopAllRespClientRequestProcessor();
  }

  private void close() {
    try {
      serverSocketChannel.close();
    } catch (IOException e) {
      logger.atWarning().withCause(e).log("Error closing the ServerSocketChannel");
    }
  }

  private void stopAllRespClientRequestProcessor() {
    for (var future : respClientRequestProcessorFutures) {
      future.cancel(true);
    }
  }
}
