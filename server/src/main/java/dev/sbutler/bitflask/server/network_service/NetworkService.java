package dev.sbutler.bitflask.server.network_service;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.server.client_handling_service.ClientRequestHandler;
import dev.sbutler.bitflask.server.client_handling_service.ClientRequestModule;
import dev.sbutler.bitflask.server.command_processing_service.CommandProcessingModule;
import dev.sbutler.bitflask.server.configuration.ServerModule;
import dev.sbutler.bitflask.storage.StorageServiceModule;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

public final class NetworkService extends AbstractExecutionThreadService {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ExecutorService executorService;
  private final ServerSocketChannel serverSocketChannel;
  private Injector rootInjector;

  @Inject
  NetworkService(ExecutorService executorService,
      ServerSocketChannel serverSocketChannel) {
    this.executorService = executorService;
    this.serverSocketChannel = serverSocketChannel;
  }

  @Override
  protected void startUp() {
    rootInjector = Guice.createInjector(
        ServerModule.getInstance(),
        StorageServiceModule.getInstance(),
        new CommandProcessingModule()
    );
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
    try {
      serverSocketChannel.close();
    } catch (IOException e) {
      logger.atSevere().withCause(e).log("Error closing NetworkService's ServerSocketChannel");
    }
  }

  private void acceptAndExecuteNextClientConnection() throws IOException {
    try {
      SocketChannel socketChannel = serverSocketChannel.accept();
      Injector injector = createChildInjector(socketChannel);
      ClientRequestHandler clientRequestHandler = injector.getInstance(
          ClientRequestHandler.class);

      printClientConnectionInfo(socketChannel);

      executorService.execute(clientRequestHandler);
    } catch (ClosedChannelException e) {
      logger.atInfo().log("ServerSocketChannel closed");
    }
  }

  private Injector createChildInjector(SocketChannel clientSocketChannel) {
    return rootInjector.createChildInjector(new ClientRequestModule(clientSocketChannel));
  }

  public void close() throws IOException {
    serverSocketChannel.close();
  }

  private void printClientConnectionInfo(SocketChannel socketChannel) throws IOException {
    logger.atInfo()
        .log("Received incoming client connection from [%s]", socketChannel.getRemoteAddress());
  }
}
