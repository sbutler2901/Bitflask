package dev.sbutler.bitflask.server;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.server.configuration.ServerModule;
import dev.sbutler.bitflask.server.network_service.NetworkService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

/**
 * Provides support for getting and setting key value pairs with persistence
 */
public class Server {

  private Server() {
  }

  public static void main(String[] args) {
    printConfigInfo();

    Injector injector = Guice.createInjector(ServerModule.getInstance());
    ExecutorService executorService = injector.getInstance(ExecutorService.class);
    NetworkService networkService = injector.getInstance(NetworkService.class);

    registerShutdownHook(networkService);

    try {
      executorService.submit(networkService).get();
    } catch (InterruptedException | ExecutionException e) {
      // todo: determine best way to handle these exceptions
      networkService.shutdownAndAwaitTermination();
      e.printStackTrace();
    }
  }

  private static void registerShutdownHook(NetworkService networkService) {
    Runtime.getRuntime().addShutdownHook(new Thread(networkService::shutdownAndAwaitTermination));
  }

  private static void printConfigInfo() {
    System.out
        .printf("Runtime processors available (%s)%n", Runtime.getRuntime().availableProcessors());
  }
}
