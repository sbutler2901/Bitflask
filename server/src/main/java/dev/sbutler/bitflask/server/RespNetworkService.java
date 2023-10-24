package dev.sbutler.bitflask.server;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;

/**
 * Handles accepting incoming RESP based client connections and spawning a {@link
 * RespClientService}.
 *
 * <p>If this service is shutdown, all spawned RespClientHandlingServices that are still running
 * will also be shutdown.
 */
@Singleton
final class RespNetworkService extends AbstractExecutionThreadService {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ListeningExecutorService listeningExecutorService;
  private final ServerSocketChannel serverSocketChannel;
  private final RespClientService.Factory clientHandlingServiceFactory;
  private final Set<RespClientService> runningRespClientServices = ConcurrentHashMap.newKeySet();

  @Inject
  RespNetworkService(
      ListeningExecutorService listeningExecutorService,
      RespClientService.Factory clientHandlingServiceFactory,
      @Assisted ServerSocketChannel serverSocketChannel) {
    this.listeningExecutorService = listeningExecutorService;
    this.clientHandlingServiceFactory = clientHandlingServiceFactory;
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

        RespClientService respClientService = clientHandlingServiceFactory.create(socketChannel);
        startRespClientHandlingService(respClientService);
      }
    } catch (AsynchronousCloseException ignored) {
      // Service shutdown externally
    } catch (IOException e) {
      logger.atSevere().withCause(e).log(
          "Failed to create RespClientService for incoming client connection. Shutting down.");
    } finally {
      triggerShutdown();
    }
  }

  private void startRespClientHandlingService(RespClientService respClientService) {
    runningRespClientServices.add(respClientService);

    // Clean up after service has reached a terminal state on its own
    respClientService
        .startAsync()
        .addListener(
            new Listener() {
              @Override
              public void terminated(@Nonnull State from) {
                runningRespClientServices.remove(respClientService);
              }

              @Override
              public void failed(@Nonnull State from, @Nonnull Throwable failure) {
                runningRespClientServices.remove(respClientService);
              }
            },
            listeningExecutorService);
  }

  @Override
  protected void triggerShutdown() {
    close();
    stopAllRespClientHandlingServices();
  }

  private void close() {
    try {
      serverSocketChannel.close();
    } catch (IOException e) {
      logger.atWarning().withCause(e).log("Error closing the ServerSocketChannel");
    }
  }

  private void stopAllRespClientHandlingServices() {
    runningRespClientServices.forEach(RespClientService::stopAsync);
    runningRespClientServices.forEach(RespClientService::awaitTerminated);
  }
}
