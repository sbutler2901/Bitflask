package dev.sbutler.bitflask.server.network_service;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.server.network_service.client_handling_service.ClientHandlingService;
import dev.sbutler.bitflask.server.network_service.client_handling_service.ClientHandlingServiceModule;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Handles accepting incoming client requests and submitting them for processing.
 */
@Singleton
public final class NetworkService extends AbstractExecutionThreadService {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ServerSocketChannel serverSocketChannel;
  private final ExecutorService executorService;
  private final HashSet<ClientHandlingService> runningClientHandlingServices = new HashSet<>();
  private volatile boolean isRunning = true;

  @Inject
  NetworkService(ServerSocketChannel serverSocketChannel, ExecutorService executorService) {
    this.serverSocketChannel = serverSocketChannel;
    this.executorService = executorService;
  }

  @Override
  protected void startUp() {
    logger.atInfo().log("Prepared to accept incoming connections");
  }

  @Override
  protected void run() throws IOException {
    while (isRunning && serverSocketChannel.isOpen()) {
      acceptAndExecuteNextClientConnection();
    }
  }

  private void acceptAndExecuteNextClientConnection() throws IOException {
    try {
      SocketChannel socketChannel = serverSocketChannel.accept();
      Injector injector = Guice.createInjector(
          new ClientHandlingServiceModule(socketChannel));
      ClientHandlingService clientHandlingService = injector.getInstance(
          ClientHandlingService.class);

      logger.atInfo()
          .log("Received incoming client connection from [%s]", socketChannel.getRemoteAddress());

      submitClientHandlingService(clientHandlingService);
    } catch (ClosedChannelException e) {
      logger.atInfo().log("ServerSocketChannel closed");
    }
  }

  private void submitClientHandlingService(ClientHandlingService clientHandlingService) {
    Futures.submit(clientHandlingService, executorService)
        .addListener(() -> runningClientHandlingServices.remove(clientHandlingService),
            executorService);
    runningClientHandlingServices.add(clientHandlingService);
  }

  @SuppressWarnings("UnstableApiUsage")
  @Override
  protected void triggerShutdown() {
    System.out.println("NetworkService shutdown triggered");
    isRunning = false;
    try {
      serverSocketChannel.close();
    } catch (IOException e) {
      System.err.println("Error closing NetworkService's ServerSocketChannel" + e);
    }
    runningClientHandlingServices.forEach(ClientHandlingService::close);
    System.out.println("NetworkService completed shutdown");
  }
}
