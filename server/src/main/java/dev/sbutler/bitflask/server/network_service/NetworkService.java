package dev.sbutler.bitflask.server.network_service;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.ListeningExecutorService;
import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Handles accepting incoming client requests and submitting them for processing.
 */
@Singleton
public final class NetworkService extends AbstractExecutionThreadService {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  public static class Factory {

    private final ListeningExecutorService listeningExecutorService;
    private final ClientHandlingService.Factory clientHandlingServiceFactory;

    @Inject
    Factory(ListeningExecutorService listeningExecutorService,
        ClientHandlingService.Factory clientHandlingServiceFactory) {
      this.listeningExecutorService = listeningExecutorService;
      this.clientHandlingServiceFactory = clientHandlingServiceFactory;
    }

    public NetworkService create(ServerSocketChannel serverSocketChannel) {
      return new NetworkService(
          listeningExecutorService,
          serverSocketChannel,
          clientHandlingServiceFactory);
    }
  }

  private final ListeningExecutorService listeningExecutorService;
  private final ServerSocketChannel serverSocketChannel;
  private final ClientHandlingService.Factory clientHandlingServiceFactory;
  private final Set<ClientHandlingService> runningClientHandlingServices = ConcurrentHashMap.newKeySet();

  private volatile boolean isRunning = true;

  NetworkService(ListeningExecutorService listeningExecutorService,
      ServerSocketChannel serverSocketChannel,
      ClientHandlingService.Factory clientHandlingServiceFactory) {
    this.listeningExecutorService = listeningExecutorService;
    this.serverSocketChannel = serverSocketChannel;
    this.clientHandlingServiceFactory = clientHandlingServiceFactory;
  }

  @Override
  protected void run() throws IOException {
    while (isRunning && serverSocketChannel.isOpen()
        && !Thread.currentThread().isInterrupted()) {
      SocketChannel socketChannel = serverSocketChannel.accept();
      logger.atInfo().log(
          "Received incoming client connection from [%s]",
          socketChannel.getRemoteAddress());

      ClientHandlingService clientHandlingService =
          clientHandlingServiceFactory.create(socketChannel);
      startClientHandlingService(clientHandlingService);
    }
  }

  private void startClientHandlingService(ClientHandlingService clientHandlingService) {
    runningClientHandlingServices.add(clientHandlingService);

    // Clean up after service has reached a terminal state on its own
    clientHandlingService
        .startAsync()
        .addListener(new Listener() {
          @Override
          public void terminated(@Nonnull State from) {
            runningClientHandlingServices.remove(clientHandlingService);
          }

          @Override
          public void failed(@Nonnull State from, @Nonnull Throwable failure) {
            runningClientHandlingServices.remove(clientHandlingService);
          }
        }, listeningExecutorService);
  }

  @SuppressWarnings("UnstableApiUsage")
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
      logger.atWarning().withCause(e)
          .log("Error closing the ServerSocketChannel");
    }
  }

  private void stopAllClientHandlingServices() {
    runningClientHandlingServices.forEach(ClientHandlingService::stopAsync);
    runningClientHandlingServices.forEach(ClientHandlingService::awaitTerminated);
  }
}
