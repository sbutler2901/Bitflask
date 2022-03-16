package dev.sbutler.bitflask.server;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.server.configuration.ServerModule;
import dev.sbutler.bitflask.server.configuration.logging.InjectLogger;
import dev.sbutler.bitflask.server.network_service.NetworkService;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import org.slf4j.Logger;

/**
 * Provides support for getting and setting key value pairs with persistence
 */
public class Server {

  @InjectLogger
  Logger logger;

  private final ExecutorService executorService;
  private final NetworkService networkService;

  @Inject
  private Server(ExecutorService executorService, NetworkService networkService) {
    this.executorService = executorService;
    this.networkService = networkService;
  }

  public static void main(String[] args) {
    Injector injector = Guice.createInjector(ServerModule.getInstance());
    Server server = injector.getInstance(Server.class);

    server.run();
  }

  void run() {
    printConfigInfo();
    registerShutdownHook();
    startNetworkService();
  }

  private void startNetworkService() {
    try {
      executorService.submit(networkService).get();
    } catch (InterruptedException | ExecutionException e) {
      // todo: determine best way to handle these exceptions
      logger.error("Issue executing NetworkService", e);
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
    try {
      networkService.close();
    } catch (IOException e) {
      logger.error("Error closing NetworkService", e);
    }
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

  private void printConfigInfo() {
    logger.info("Runtime processors available {}", Runtime.getRuntime().availableProcessors());
  }
}
