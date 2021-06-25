package bitflask.server;

import bitflask.server.storage.Storage;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Provides support for getting and setting key value pairs with persistence
 */
public class Server {

  public static final int PORT = 9090;
  private static final int NUM_THREADS = 4;

  private final ServerSocket serverSocket;
  private final Storage storage;
  private final ThreadPoolExecutor threadPool;

  Server() throws IOException {
    this.threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUM_THREADS);
    this.storage = new Storage(threadPool);
    this.serverSocket = new ServerSocket(PORT);
  }

  Server(Storage storage) throws IOException {
    this.threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUM_THREADS);
    this.storage = storage;
    this.serverSocket = new ServerSocket(PORT);
  }

  public static void main(String[] args) {
    try {
      Server server = new Server();
      server.start();
      System.exit(0);
    } catch (IOException e) {
      System.out.println("Unable to initialize storage engine. Terminating");
      e.printStackTrace();
      System.exit(1);
    }
  }

  private void printConfigInfo() {
    System.out
        .printf("Runtime processors available (%s)%n", Runtime.getRuntime().availableProcessors());
  }

  private void start() {
    System.out.println("Welcome to Bitflask!");
    printConfigInfo();

    try {
      while (true) {
        Socket clientSocket = serverSocket.accept();
        RequestHandler clientRequestHandler = new RequestHandler(clientSocket, storage);

        System.out.println(
            "S: Received incoming client connection from " + clientSocket.getInetAddress() + ":"
                + clientSocket.getPort());

        this.threadPool.execute(clientRequestHandler);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      threadPool.shutdown();
    }
  }
}
