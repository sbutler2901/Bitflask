package dev.sbutler.bitflask.server;

import com.google.common.collect.ImmutableSet;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.common.util.concurrent.ServiceManager.Listener;
import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.server.configuration.ServerModule;
import dev.sbutler.bitflask.server.network_service.NetworkService;
import dev.sbutler.bitflask.storage.StorageService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;

/**
 * Provides support for getting and setting key value pairs with persistence
 */
public class Server {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private Server() {
  }

  public static void main(String[] args) {
    Injector injector = Guice.createInjector(ServerModule.getInstance());
    ImmutableSet<Service> services = ImmutableSet.of(
        injector.getInstance(NetworkService.class),
        injector.getInstance(StorageService.class)
    );
    ServiceManager serviceManager = new ServiceManager(services);
    addServiceManagerListener(serviceManager);
    registerShutdownHook(serviceManager);
    printConfigInfo();
    serviceManager.startAsync();
  }

  private static void addServiceManagerListener(ServiceManager serviceManager) {
    serviceManager.addListener(
        new Listener() {
          public void stopped() {
            logger.atInfo().log("All services have stopped. Exiting");
            System.exit(0);
          }

          public void healthy() {
            logger.atInfo().log("All services have been initialized and are healthy");
          }

          public void failure(@Nonnull Service service) {
            logger.atSevere().log("[%d] failed. Exiting...", service.getClass());
            System.exit(1);
          }
        },
        MoreExecutors.directExecutor());
  }

  private static void registerShutdownHook(ServiceManager serviceManager) {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      // Give the services 5 seconds to stop to ensure that we are responsive to shut down
      // requests.
      try {
        serviceManager.stopAsync().awaitStopped(5, TimeUnit.SECONDS);
      } catch (TimeoutException timeout) {
        // stopping timed out
        logger.atSevere().withCause(timeout).log("ServiceManager timed out while stopping");
      }
    }));
  }

  private static void printConfigInfo() {
    logger.atInfo()
        .log("Runtime processors available [%d]", Runtime.getRuntime().availableProcessors());
  }
}
