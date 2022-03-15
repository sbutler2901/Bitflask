package dev.sbutler.bitflask.server.network_service;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import dev.sbutler.bitflask.resp.network.RespNetworkModule;
import dev.sbutler.bitflask.server.client_connection.ClientConnectionModule;
import dev.sbutler.bitflask.server.client_processing.ClientProcessingModule;
import dev.sbutler.bitflask.server.client_processing.ClientRequestHandler;
import dev.sbutler.bitflask.server.command_processing.CommandProcessingModule;
import dev.sbutler.bitflask.server.configuration.ServerModule;
import dev.sbutler.bitflask.storage.StorageModule;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;

public class NetworkServiceImpl implements NetworkService {

  private static final String SERVER_SOCKET_CLOSED = "Server socket closed";
  private static final String SERVER_SOCKET_FAILURE = "Failed to accept incoming client connections";

  private final ExecutorService executorService;
  private final ServerSocketChannel serverSocketChannel;
  private Injector rootInjector;

  @Inject
  public NetworkServiceImpl(ExecutorService executorService,
      ServerSocketChannel serverSocketChannel) {
    this.executorService = executorService;
    this.serverSocketChannel = serverSocketChannel;
  }

  @Override
  public void run() {
    initialize();

    try {
      while (serverSocketChannel.isOpen()) {
        acceptAndExecuteNextClientConnection();
      }
    } catch (IOException e) {
      System.out.println(SERVER_SOCKET_FAILURE);
    }
  }

  private void initialize() {
    rootInjector = Guice.createInjector(
        ServerModule.getInstance(),
        StorageModule.getInstance()
    );
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
      System.out.println(SERVER_SOCKET_CLOSED);
    }
  }

  private Injector createChildInjector(SocketChannel clientSocketChannel) {
    return rootInjector.createChildInjector(
        new CommandProcessingModule(),
        new ClientConnectionModule(clientSocketChannel),
        new RespNetworkModule(),
        new ClientProcessingModule()
    );
  }

  public void close() throws IOException {
    serverSocketChannel.close();
  }

  private void printClientConnectionInfo(SocketChannel socketChannel) throws IOException {
    System.out.println(
        "S: Received incoming client connection from " + socketChannel.getRemoteAddress());
  }
}
