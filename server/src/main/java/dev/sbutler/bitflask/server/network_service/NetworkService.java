package dev.sbutler.bitflask.server.network_service;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.server.network_service.client_handling_service.ClientHandlingService;
import dev.sbutler.bitflask.server.network_service.client_handling_service.ClientHandlingServiceModule;
import dev.sbutler.bitflask.storage.StorageCommandDispatcher;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class NetworkService extends AbstractExecutionThreadService {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ServerSocketChannel serverSocketChannel;
  private final ExecutorService executorService;
  private final StorageCommandDispatcher storageCommandDispatcher;
  private final List<ClientHandlingService> runningClientHandlingServices = new ArrayList<>();

  @Inject
  NetworkService(ServerSocketChannel serverSocketChannel, ExecutorService executorService,
      StorageCommandDispatcher storageCommandDispatcher) {
    this.serverSocketChannel = serverSocketChannel;
    this.executorService = executorService;
    this.storageCommandDispatcher = storageCommandDispatcher;
  }

  @Override
  protected void startUp() {
    logger.atInfo().log("Prepared to accept incoming connections");
  }

  @Override
  protected void run() {
    try {
      while (serverSocketChannel.isOpen()) {
        acceptAndExecuteNextClientConnection();
      }
    } catch (IOException e) {
      logger.atSevere().withCause(e).log("Failed to accept incoming client connections");
    }
  }

  @SuppressWarnings("UnstableApiUsage")
  @Override
  protected void triggerShutdown() {
    System.out.println("NetworkService shutdown triggered");
    try {
      serverSocketChannel.close();
    } catch (IOException e) {
      System.err.println("Error closing NetworkService's ServerSocketChannel" + e);
    }
    Iterator<ClientHandlingService> iterator = runningClientHandlingServices.iterator();
    while (iterator.hasNext()) {
      ClientHandlingService next = iterator.next();
      next.close();
      iterator.remove();
    }
    System.out.println("NetworkService completed shutdown");
  }

  private void acceptAndExecuteNextClientConnection() throws IOException {
    try {
      SocketChannel socketChannel = serverSocketChannel.accept();
      Injector injector = Guice.createInjector(
          new ClientHandlingServiceModule(executorService, socketChannel,
              storageCommandDispatcher));
      ClientHandlingService clientHandlingService = injector.getInstance(
          ClientHandlingService.class);

      printClientConnectionInfo(socketChannel);

      Futures.submit(clientHandlingService, executorService);
      runningClientHandlingServices.add(clientHandlingService);
    } catch (ClosedChannelException e) {
      logger.atInfo().log("ServerSocketChannel closed");
    }
  }

  private void printClientConnectionInfo(SocketChannel socketChannel) throws IOException {
    logger.atInfo()
        .log("Received incoming client connection from [%s]", socketChannel.getRemoteAddress());
  }
}
