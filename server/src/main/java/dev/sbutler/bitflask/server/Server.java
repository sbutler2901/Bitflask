package dev.sbutler.bitflask.server;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.sun.jdi.InternalException;
import dev.sbutler.bitflask.server.client_processing.ClientRequestHandler;
import dev.sbutler.bitflask.server.configuration.ServerModule;
import dev.sbutler.bitflask.server.storage.Storage;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Provides support for getting and setting key value pairs with persistence
 */
public class Server {

  private static final String GREETING_MSG = "Welcome to Bitflask!";
  private static final String INITIALIZATION_FAILURE = "Failed to initialize the server";
  private static final String TERMINATION_FAILURE = "Failed to properly terminate the server";
  private static final String CLIENT_CONNECTION_FAILURE = "Failed to accept incoming client connection";

  private final ServerSocket serverSocket;
  private final Storage storage;
  private final ThreadPoolExecutor threadPoolExecutor;

  private boolean shouldContinueRunning = true;

  @Inject
  Server(ThreadPoolExecutor threadPoolExecutor, Storage storage, ServerSocket serverSocket) {
    this.threadPoolExecutor = threadPoolExecutor;
    this.storage = storage;
    this.serverSocket = serverSocket;
  }

  public static void main(String[] args) {
    Injector injector = Guice.createInjector(new ServerModule());

    Server server = injector.getInstance(Server.class);
    server.start();
    server.close();
  }

  public void start() {
    System.out.println(GREETING_MSG);
    printConfigInfo();

    try {
      while (shouldContinueRunning) {
        Socket clientSocket = serverSocket.accept();
        ClientRequestHandler clientRequestHandler = new ClientRequestHandler(clientSocket, storage);

        printClientConnectionInfo(clientSocket);

        this.threadPoolExecutor.execute(clientRequestHandler);
      }
    } catch (IOException e) {
      throw new InternalException(CLIENT_CONNECTION_FAILURE);
    }
  }

  public void close() {
    try {
      shouldContinueRunning = false;
      threadPoolExecutor.shutdown();
      serverSocket.close();
    } catch (IOException e) {
      throw new InternalException(TERMINATION_FAILURE);
    }
  }

  private void printConfigInfo() {
    System.out
        .printf("Runtime processors available (%s)%n", Runtime.getRuntime().availableProcessors());
  }

  private void printClientConnectionInfo(Socket clientSocket) {
    System.out.println(
        "S: Received incoming client connection from " + clientSocket.getInetAddress() + ":"
            + clientSocket.getPort());
  }

}
