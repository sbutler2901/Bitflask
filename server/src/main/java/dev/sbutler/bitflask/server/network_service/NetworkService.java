package dev.sbutler.bitflask.server.network_service;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import dev.sbutler.bitflask.resp.network.RespNetworkModule;
import dev.sbutler.bitflask.server.client_connection.ClientConnectionModule;
import dev.sbutler.bitflask.server.client_processing.ClientProcessingModule;
import dev.sbutler.bitflask.server.client_processing.ClientRequestHandler;
import dev.sbutler.bitflask.server.command_processing.CommandProcessingModule;
import dev.sbutler.bitflask.server.configuration.ServerModule;
import dev.sbutler.bitflask.storage.StorageModule;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;

@Singleton
public class NetworkService implements Runnable {

  private static final String SERVER_SOCKET_CLOSED = "Server socket closed";
  private static final String SERVER_SOCKET_FAILURE = "Failed to accept incoming client connections";

  private final ExecutorService executorService;
  private final ServerSocket serverSocket;
  private Injector rootInjector;

  @Inject
  public NetworkService(ExecutorService executorService, ServerSocket serverSocket) {
    this.executorService = executorService;
    this.serverSocket = serverSocket;
  }

  @Override
  public void run() {
    initialize();

    try {
      while (!serverSocket.isClosed()) {
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
      Socket clientSocket = serverSocket.accept();
      Injector injector = createChildInjector(clientSocket);
      ClientRequestHandler clientRequestHandler = injector.getInstance(
          ClientRequestHandler.class);

      printClientConnectionInfo(clientSocket);

      executorService.execute(clientRequestHandler);
    } catch (SocketException e) {
      System.out.println(SERVER_SOCKET_CLOSED);
    }
  }

  private Injector createChildInjector(Socket clientSocket) {
    return rootInjector.createChildInjector(
        new CommandProcessingModule(),
        new ClientConnectionModule(clientSocket),
        new RespNetworkModule(),
        new ClientProcessingModule()
    );
  }

  public void close() {
    System.out.println("NetworkService: closing");
    try {
      serverSocket.close();
    } catch (IOException e) {
      System.out.println("Exception thrown while closing serverSocket" + e.getMessage());
    }
  }

  private void printClientConnectionInfo(Socket clientSocket) {
    System.out.println(
        "S: Received incoming client connection from " + clientSocket.getInetAddress() + ":"
            + clientSocket.getPort());
  }
}
