package dev.sbutler.bitflask.server;

import dev.sbutler.bitflask.server.client_processing.ClientRequestHandler;
import dev.sbutler.bitflask.server.storage.Storage;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Provides support for getting and setting key value pairs with persistence
 */
public class Server {

  private static final int PORT = 9090;
  private static final int NUM_THREADS = 4;

  private static final String GREETING_MSG = "Welcome to Bitflask!";
  private static final String INITIALIZATION_FAILURE = "Failed to initialize the server";

  private final ServerSocket serverSocket;
  private final Storage storage;
  private final ThreadPoolExecutor threadPoolExecutor;

  private boolean shouldContinueRunning = true;

  Server(ThreadPoolExecutor threadPoolExecutor, Storage storage, ServerSocket serverSocket) {
    this.threadPoolExecutor = threadPoolExecutor;
    this.storage = storage;
    this.serverSocket = serverSocket;
  }

  public static void main(String[] args) {
    Server server;

    try {
      server = initializeServer();
    } catch (IOException e) {
      System.out.println(INITIALIZATION_FAILURE);
      e.printStackTrace();
      System.exit(1);
      return;
    }

    server.start();
    server.close();
  }

  private static Server initializeServer() throws IOException {
    ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(
        NUM_THREADS);
    Storage storage = new Storage(threadPoolExecutor);
    ServerSocket serverSocket = new ServerSocket(PORT);
    return new Server(threadPoolExecutor, storage, serverSocket);
  }

  public void start() {
    System.out.println(GREETING_MSG);
    printConfigInfo();

    try {
      while (shouldContinueRunning) {
        Socket clientSocket = serverSocket.accept();
        ClientRequestHandler clientRequestHandler = new ClientRequestHandler(clientSocket, storage);

        System.out.println(
            "S: Received incoming client connection from " + clientSocket.getInetAddress() + ":"
                + clientSocket.getPort());

        this.threadPoolExecutor.execute(clientRequestHandler);
      }
    } catch (IOException | RejectedExecutionException e) {
      e.printStackTrace();
    }
  }

  public void close() {
    try {
      shouldContinueRunning = false;
      threadPoolExecutor.shutdown();
      serverSocket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void printConfigInfo() {
    System.out
        .printf("Runtime processors available (%s)%n", Runtime.getRuntime().availableProcessors());
  }

}
