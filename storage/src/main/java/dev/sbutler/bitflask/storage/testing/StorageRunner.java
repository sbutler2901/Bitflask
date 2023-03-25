package dev.sbutler.bitflask.storage.testing;

import static com.google.common.util.concurrent.MoreExecutors.shutdownAndAwaitTermination;

import com.google.common.collect.ImmutableSet;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.common.concurrency.VirtualThreadConcurrencyModule;
import dev.sbutler.bitflask.common.configuration.ConfigurationsBuilder;
import dev.sbutler.bitflask.storage.StorageService;
import dev.sbutler.bitflask.storage.StorageServiceModule;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurationsConstants;
import java.time.Duration;
import java.util.ResourceBundle;
import java.util.concurrent.TimeoutException;

public final class StorageRunner {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  public static void main(String[] args) {
    try {
      StorageConfigurations configurations = initializeConfiguration(args);
      printConfigInfo(configurations);

      Injector injector = Guice.createInjector(ImmutableSet.of(
          new VirtualThreadConcurrencyModule(),
          new StorageServiceModule(configurations)));

      ServiceManager serviceManager = new ServiceManager(ImmutableSet.of(
          injector.getInstance(StorageService.class)));
      ListeningExecutorService listeningExecutorService =
          injector.getInstance(ListeningExecutorService.class);

      registerShutdownHook(serviceManager, listeningExecutorService);

      serviceManager.startAsync().awaitHealthy(Duration.ofSeconds(5));

      StorageTester storageTester = injector.getInstance(StorageTester.class);
      storageTester.run();

      System.exit(0);
    } catch (Exception e) {
      logger.atSevere().withCause(e).log("Catastrophic error starting StorageRunner");
    }
  }

  @SuppressWarnings("UnstableApiUsage")
  private static void registerShutdownHook(ServiceManager serviceManager,
      ListeningExecutorService listeningExecutorService) {
    Runtime.getRuntime().addShutdownHook(Thread.ofVirtual().unstarted(() -> {
      System.out.println("Starting StorageRunner shutdown hook");
      // Give the services 5 seconds to stop to ensure that we are responsive to shut down
      // requests.
      try {
        serviceManager.stopAsync().awaitStopped(Duration.ofSeconds(5));
      } catch (TimeoutException timeout) {
        // stopping timed out
        System.err.println("ServiceManager timed out while stopping" + timeout);
      }
      shutdownAndAwaitTermination(listeningExecutorService, Duration.ofSeconds(5));
      System.out.println("Shutdown hook completed");
    }));
  }

  private static StorageConfigurations initializeConfiguration(String[] args) {
    ResourceBundle resourceBundle = ResourceBundle.getBundle("test");
    ConfigurationsBuilder configurationsBuilder = new ConfigurationsBuilder(args, resourceBundle);

    StorageConfigurations storageConfigurations = new StorageConfigurations();
    configurationsBuilder.buildAcceptingUnknownOptions(storageConfigurations,
        StorageConfigurationsConstants.STORAGE_FLAG_TO_CONFIGURATION_MAP);
    return storageConfigurations;
  }

  private static void printConfigInfo(StorageConfigurations storageConfigurations) {
    logger.atInfo().log("Using java version [%s]", System.getProperty("java.version"));
    logger.atInfo()
        .log("Runtime processors available [%d]", Runtime.getRuntime().availableProcessors());

    logger.atInfo().log(storageConfigurations.toString());
  }
}
