package dev.sbutler.bitflask.server;

import static com.google.common.util.concurrent.MoreExecutors.shutdownAndAwaitTermination;

import com.google.common.collect.ImmutableSet;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.common.util.concurrent.ServiceManager.Listener;
import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.common.configuration.ConfigurationsBuilder;
import dev.sbutler.bitflask.server.configuration.ServerConfigurations;
import dev.sbutler.bitflask.server.configuration.ServerConfigurationsConstants;
import dev.sbutler.bitflask.server.configuration.ServerModule;
import dev.sbutler.bitflask.server.network_service.NetworkService;
import dev.sbutler.bitflask.storage.StorageService;
import dev.sbutler.bitflask.storage.StorageServiceModule;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurationsConstants;
import java.time.Duration;
import java.time.Instant;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;

/**
 * Bootstraps the server initializing all necessary services, starting them, and handling shutdown
 * cleanup.
 */
public final class Server {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static Instant executionStarted;

  private Server() {
  }

  public static void main(String[] args) {
    try {
      executionStarted = Instant.now();
      printConfigInfo();
      initializeConfigurations(args);
      Injector injector = Guice.createInjector(ServerModule.getInstance());
      ImmutableSet<Service> services = ImmutableSet.of(
          injector.getInstance(StorageService.class),
          injector.getInstance(NetworkService.class));
      ServiceManager serviceManager = new ServiceManager(services);
      ListeningExecutorService listeningExecutorService =
          injector.getInstance(ListeningExecutorService.class);
      addServiceManagerListener(serviceManager, listeningExecutorService);
      registerShutdownHook(serviceManager, listeningExecutorService);
      serviceManager.startAsync();
    } catch (Exception e) {
      logger.atSevere().withCause(e).log("Server catastrophic failure");
    }
  }

  private static void addServiceManagerListener(ServiceManager serviceManager,
      ListeningExecutorService listeningExecutorService) {
    serviceManager.addListener(
        new Listener() {
          public void stopped() {
            System.out.println("Server: All services have stopped.");
          }

          public void healthy() {
            logger.atInfo().log("All services have been initialized and are healthy");
            logTimeFromStart();
          }

          public void failure(@Nonnull Service service) {
            logger.atSevere().withCause(service.failureCause())
                .log("[%s] failed.", service.getClass());
            serviceManager.stopAsync();
          }
        }, listeningExecutorService);
  }

  @SuppressWarnings("UnstableApiUsage")
  private static void registerShutdownHook(ServiceManager serviceManager,
      ListeningExecutorService listeningExecutorService) {
    Runtime.getRuntime().addShutdownHook(Thread.ofVirtual().unstarted(() -> {
      System.out.println("Starting Server shutdown hook");
      // Give the services 5 seconds to stop to ensure that we are responsive to shut down
      // requests.
      try {
        serviceManager.stopAsync().awaitStopped(5, TimeUnit.SECONDS);
      } catch (TimeoutException timeout) {
        // stopping timed out
        System.err.println("ServiceManager timed out while stopping" + timeout);
      }
      shutdownAndAwaitTermination(listeningExecutorService, Duration.ofSeconds(5));
      System.out.println("Shutdown hook completed");
    }));
  }

  private static void initializeConfigurations(String[] args) {
    ResourceBundle resourceBundle = ResourceBundle.getBundle("config");
    ConfigurationsBuilder configsBuilder = new ConfigurationsBuilder(args, resourceBundle);

    ServerConfigurations serverConfigurations = new ServerConfigurations();
    configsBuilder.buildAcceptingUnknownOptions(
        serverConfigurations,
        ServerConfigurationsConstants.SERVER_FLAG_TO_CONFIGURATION_MAP);
    ServerModule.setServerConfiguration(serverConfigurations);

    StorageConfigurations storageConfigurations = new StorageConfigurations();
    configsBuilder.buildAcceptingUnknownOptions(storageConfigurations,
        StorageConfigurationsConstants.STORAGE_FLAG_TO_CONFIGURATION_MAP);
    StorageServiceModule.setStorageConfiguration(storageConfigurations);

    logger.atInfo().log(serverConfigurations.toString());
    logger.atInfo().log(storageConfigurations.toString());
  }

  private static void printConfigInfo() {
    logger.atInfo().log("Using java version [%s]", System.getProperty("java.version"));
    logger.atInfo()
        .log("Runtime processors available [%d]", Runtime.getRuntime().availableProcessors());
  }

  private static void logTimeFromStart() {
    Instant now = Instant.now();
    Duration startupTime = Duration.between(executionStarted, now);
    logger.atInfo().log("Time to startup: [%d]millis", startupTime.toMillis());
  }
}
