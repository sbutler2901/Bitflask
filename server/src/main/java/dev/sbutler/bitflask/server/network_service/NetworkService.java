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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class NetworkService implements Runnable {

  private static final String GREETING_MSG = "Welcome to Bitflask!";
  private static final String SERVER_SOCKET_FAILURE = "Failed to accept incoming client connections";

  private final ExecutorService executorService;
  private final ServerSocket serverSocket;
  private Injector rootInjector;

  private volatile boolean shouldContinueRunning = true;

  @Inject
  public NetworkService(ExecutorService executorService, ServerSocket serverSocket) {
    this.executorService = executorService;
    this.serverSocket = serverSocket;
  }

  @Override
  public void run() {
    initialize();

    try {
      while (shouldContinueRunning) {
        acceptAndExecuteNextClientConnection();
      }
    } catch (IOException e) {
      System.out.println(SERVER_SOCKET_FAILURE);
      shutdownAndAwaitTermination();
    }
  }

  private void initialize() {
    rootInjector = Guice.createInjector(
        ServerModule.getInstance(),
        StorageModule.getInstance()
    );
    System.out.println(GREETING_MSG);
  }

  private void acceptAndExecuteNextClientConnection() throws IOException {
    Socket clientSocket = serverSocket.accept();

    Injector injector = createChildInjector(clientSocket);
    ClientRequestHandler clientRequestHandler = injector.getInstance(
        ClientRequestHandler.class);

    printClientConnectionInfo(clientSocket);

    executorService.execute(clientRequestHandler);
  }

  private Injector createChildInjector(Socket clientSocket) {
    return rootInjector.createChildInjector(
        new CommandProcessingModule(),
        new ClientConnectionModule(clientSocket),
        new RespNetworkModule(),
        new ClientProcessingModule()
    );
  }

  public void shutdownAndAwaitTermination() {
    System.out.println("Shutdown activated");
    shouldContinueRunning = false;
    executorService.shutdown(); // Disable new tasks from being submitted
    try {
      // Wait a while for existing tasks to terminate
      if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
        executorService.shutdownNow(); // Cancel currently executing tasks
        // Wait a while for tasks to respond to being cancelled
        if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
          System.err.println("Pool did not terminate");
        }
      }
    } catch (InterruptedException ex) {
      // (Re-)Cancel if current thread also interrupted
      executorService.shutdownNow();
      // Preserve interrupt status
      Thread.currentThread().interrupt();
    }
  }

  private void printClientConnectionInfo(Socket clientSocket) {
    System.out.println(
        "S: Received incoming client connection from " + clientSocket.getInetAddress() + ":"
            + clientSocket.getPort());
  }
}
