package dev.sbutler.bitflask.server;

import static com.google.common.util.concurrent.MoreExecutors.shutdownAndAwaitTermination;

import com.beust.jcommander.JCommander;
import com.google.common.collect.ImmutableSet;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.common.util.concurrent.ServiceManager.Listener;
import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.server.configuration.ServerConfiguration;
import dev.sbutler.bitflask.server.configuration.ServerModule;
import dev.sbutler.bitflask.server.network_service.NetworkService;
import dev.sbutler.bitflask.storage.StorageService;
import dev.sbutler.bitflask.storage.StorageServiceModule;
import dev.sbutler.bitflask.storage.configuration.StorageConfiguration;
import java.time.Duration;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;

/**
 * Bootstraps the server initializing all necessary services, starting them, and handling shutdown
 * cleanup.
 */
class Server {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private Server() {
  }

  public static void main(String[] argv) {
    initializeConfigurations(argv);
    Injector injector = Guice.createInjector(ServerModule.getInstance());
    ImmutableSet<Service> services = ImmutableSet.of(
        injector.getInstance(StorageService.class),
        injector.getInstance(NetworkService.class)
    );
    ServiceManager serviceManager = new ServiceManager(services);
    ExecutorService executorService = injector.getInstance(ExecutorService.class);
    addServiceManagerListener(serviceManager, executorService);
    registerShutdownHook(serviceManager, executorService);
    printConfigInfo();
    serviceManager.startAsync();
  }

  private static void addServiceManagerListener(ServiceManager serviceManager,
      ExecutorService executorService) {
    serviceManager.addListener(
        new Listener() {
          public void stopped() {
            System.out.println("All services have stopped.");
          }

          public void healthy() {
            logger.atInfo().log("All services have been initialized and are healthy");
          }

          public void failure(@Nonnull Service service) {
            System.err.printf("[%s] failed.", service.getClass());
          }
        }, executorService);
  }

  @SuppressWarnings("UnstableApiUsage")
  private static void registerShutdownHook(ServiceManager serviceManager,
      ExecutorService executorService) {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("Starting Server shutdown hook");
      // Give the services 5 seconds to stop to ensure that we are responsive to shut down
      // requests.
      try {
        serviceManager.stopAsync().awaitStopped(5, TimeUnit.SECONDS);
      } catch (TimeoutException timeout) {
        // stopping timed out
        System.err.println("ServiceManager timed out while stopping" + timeout);
      }
      shutdownAndAwaitTermination(executorService, Duration.ofSeconds(5));
      System.out.println("Shutdown hook completed");
    }));
  }

  private static void initializeConfigurations(String[] argv) {
    ResourceBundle resourceBundle = ResourceBundle.getBundle("config");
    ServerConfiguration serverConfiguration = new ServerConfiguration(resourceBundle);
    StorageConfiguration storageConfiguration = new StorageConfiguration(resourceBundle);
    JCommander.newBuilder()
        .addObject(serverConfiguration)
        .addObject(storageConfiguration)
        .build()
        .parse(argv);
    ServerModule.setServerConfiguration(serverConfiguration);
    StorageServiceModule.setStorageConfiguration(storageConfiguration);
    System.out.println("test");
  }

  private static void printConfigInfo() {
    logger.atInfo().log("Using java version [%s]", System.getProperty("java.version"));
    logger.atInfo()
        .log("Runtime processors available [%d]", Runtime.getRuntime().availableProcessors());
  }
}
