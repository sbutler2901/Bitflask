package dev.sbutler.bitflask.server.network_service;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.server.client_handling.ClientRequestHandler;
import dev.sbutler.bitflask.server.client_handling.ClientRequestModule;
import dev.sbutler.bitflask.server.command_processing.CommandProcessingModule;
import dev.sbutler.bitflask.server.configuration.ServerModule;
import dev.sbutler.bitflask.server.configuration.logging.InjectLogger;
import dev.sbutler.bitflask.storage.StorageModule;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;
import org.slf4j.Logger;

class NetworkServiceImpl implements NetworkService {

  private static final String INITIALIZED_MSG = "Prepared to accept incoming connections";
  private static final String SERVER_SOCKET_CLOSED = "Closed the server socket";
  private static final String SERVER_SOCKET_FAILURE = "Failed to accept incoming client connections";
  private static final String INCOMING_CONNECTION = "Received incoming client connection from {}";

  @InjectLogger
  Logger logger;

  private final ExecutorService executorService;
  private final ServerSocketChannel serverSocketChannel;
  private Injector rootInjector;

  @Inject
  NetworkServiceImpl(ExecutorService executorService,
      ServerSocketChannel serverSocketChannel) {
    this.executorService = executorService;
    this.serverSocketChannel = serverSocketChannel;
  }

  @Override
  public void run() {
    initialize();
    start();
  }

  private void initialize() {
    rootInjector = Guice.createInjector(
        ServerModule.getInstance(),
        StorageModule.getInstance(),
        new CommandProcessingModule()
    );
    logger.info(INITIALIZED_MSG);
  }

  private void start() {
    try {
      while (serverSocketChannel.isOpen()) {
        acceptAndExecuteNextClientConnection();
      }
    } catch (IOException e) {
      logger.error(SERVER_SOCKET_FAILURE, e);
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
      logger.info(SERVER_SOCKET_CLOSED);
    }
  }

  private Injector createChildInjector(SocketChannel clientSocketChannel) {
    return rootInjector.createChildInjector(new ClientRequestModule(clientSocketChannel));
  }

  public void close() throws IOException {
    serverSocketChannel.close();
  }

  private void printClientConnectionInfo(SocketChannel socketChannel) throws IOException {
    logger.info(INCOMING_CONNECTION, socketChannel.getRemoteAddress());
  }
}
