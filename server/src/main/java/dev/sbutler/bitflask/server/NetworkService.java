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

/** Handles accepting incoming client requests and submitting them for processing. */
@Singleton
final class NetworkService extends AbstractExecutionThreadService {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ListeningExecutorService listeningExecutorService;
  private final ServerSocketChannel serverSocketChannel;
  private final ClientHandlingService.Factory clientHandlingServiceFactory;
  private final Set<ClientHandlingService> runningClientHandlingServices =
      ConcurrentHashMap.newKeySet();

  private volatile boolean isRunning = true;

  @Inject
  NetworkService(
      ListeningExecutorService listeningExecutorService,
      ClientHandlingService.Factory clientHandlingServiceFactory,
      @Assisted ServerSocketChannel serverSocketChannel) {
    this.listeningExecutorService = listeningExecutorService;
    this.clientHandlingServiceFactory = clientHandlingServiceFactory;
    this.serverSocketChannel = serverSocketChannel;
  }

  interface Factory {
    NetworkService create(ServerSocketChannel serverSocketChannel);
  }

  @Override
  protected void run() throws IOException {
    try {
      while (isRunning && serverSocketChannel.isOpen() && !Thread.currentThread().isInterrupted()) {
        SocketChannel socketChannel = serverSocketChannel.accept();
        logger.atInfo().log(
            "Received incoming client connection from [%s]", socketChannel.getRemoteAddress());

        ClientHandlingService clientHandlingService =
            clientHandlingServiceFactory.create(socketChannel);
        startClientHandlingService(clientHandlingService);
      }
    } catch (AsynchronousCloseException ignored) {
      // Service shutdown externally
    } finally {
      triggerShutdown();
    }
  }

  private void startClientHandlingService(ClientHandlingService clientHandlingService) {
    runningClientHandlingServices.add(clientHandlingService);

    // Clean up after service has reached a terminal state on its own
    clientHandlingService
        .startAsync()
        .addListener(
            new Listener() {
              @Override
              public void terminated(@Nonnull State from) {
                runningClientHandlingServices.remove(clientHandlingService);
              }

              @Override
              public void failed(@Nonnull State from, @Nonnull Throwable failure) {
                runningClientHandlingServices.remove(clientHandlingService);
              }
            },
            listeningExecutorService);
  }

  @Override
  protected void triggerShutdown() {
    isRunning = false;
    close();
    stopAllClientHandlingServices();
  }

  private void close() {
    try {
      serverSocketChannel.close();
    } catch (IOException e) {
      logger.atWarning().withCause(e).log("Error closing the ServerSocketChannel");
    }
  }

  private void stopAllClientHandlingServices() {
    runningClientHandlingServices.forEach(ClientHandlingService::stopAsync);
    runningClientHandlingServices.forEach(ClientHandlingService::awaitTerminated);
  }
}
