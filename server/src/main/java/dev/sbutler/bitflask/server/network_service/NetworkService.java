package dev.sbutler.bitflask.server.network_service;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.server.configuration.ServerConfiguration;
import dev.sbutler.bitflask.server.network_service.client_handling_service.ClientConnectionManager;
import dev.sbutler.bitflask.server.network_service.client_handling_service.ClientHandlingService;
import dev.sbutler.bitflask.server.network_service.client_handling_service.ClientHandlingServiceChildModule;
import dev.sbutler.bitflask.server.network_service.client_handling_service.ClientHandlingServiceParentModule;
import java.io.IOException;
import java.net.InetSocketAddress;
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

  private final ExecutorService executorService;
  private final ServerConfiguration serverConfiguration;
  private ServerSocketChannel serverSocketChannel;
  private Injector parentInjector;
  private final HashSet<ClientHandlingService> runningClientHandlingServices = new HashSet<>();
  private volatile boolean isRunning = true;

  @Inject
  NetworkService(ExecutorService executorService, ServerConfiguration serverConfiguration) {
    this.executorService = executorService;
    this.serverConfiguration = serverConfiguration;
  }

  @Override
  protected void startUp() throws IOException {
    ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
    InetSocketAddress inetSocketAddress = new InetSocketAddress(serverConfiguration.getPort());
    serverSocketChannel.bind(inetSocketAddress);
    this.serverSocketChannel = serverSocketChannel;
    this.parentInjector = Guice.createInjector(new ClientHandlingServiceParentModule());
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
      ClientConnectionManager connectionManager = new ClientConnectionManager(socketChannel);
      Injector childInjector = parentInjector.createChildInjector(
          new ClientHandlingServiceChildModule(connectionManager));
      ClientHandlingService clientHandlingService = childInjector.getInstance(
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
    isRunning = false;
    try {
      serverSocketChannel.close();
    } catch (IOException e) {
      System.err.println("Error closing NetworkService's ServerSocketChannel" + e);
    }
    runningClientHandlingServices.forEach(ClientHandlingService::close);
  }
}
