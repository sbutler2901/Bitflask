package dev.sbutler.bitflask.server;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import dev.sbutler.bitflask.server.configuration.ServerModule;
import dev.sbutler.bitflask.server.network_service.NetworkServiceImpl;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Provides support for getting and setting key value pairs with persistence
 */
public class Server {

  private final ExecutorService executorService;
  private final NetworkServiceImpl networkService;

  @Inject
  private Server(ExecutorService executorService, NetworkServiceImpl networkService) {
    this.executorService = executorService;
    this.networkService = networkService;
  }

  public static void main(String[] args) {
    printConfigInfo();

    Injector injector = Guice.createInjector(ServerModule.getInstance());
    Server server = injector.getInstance(Server.class);

    server.run();
  }

  void run() {
    registerShutdownHook();

    try {
      executorService.submit(networkService).get();
    } catch (InterruptedException | ExecutionException e) {
      // todo: determine best way to handle these exceptions
      e.printStackTrace();
    }
  }

  private void registerShutdownHook() {
    Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
  }

  void shutdown() {
    shutdownNetworkService();
    shutdownExecutorServiceAndAwaitTermination();
  }

  private void shutdownNetworkService() {
    networkService.close();
  }

  private void shutdownExecutorServiceAndAwaitTermination() {
    System.out.println("Shutdown activated");
    executorService.shutdownNow();
    try {
      // Wait a while for tasks to respond to being cancelled
      if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
        System.err.println("Pool did not terminate");
      }
    } catch (InterruptedException ex) {
      // (Re-)Cancel if current thread also interrupted
      executorService.shutdownNow();
      // Preserve interrupt status
      Thread.currentThread().interrupt();
    }
  }

  private static void printConfigInfo() {
    System.out
        .printf("Runtime processors available (%s)%n", Runtime.getRuntime().availableProcessors());
  }
}
