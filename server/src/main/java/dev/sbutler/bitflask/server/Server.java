package dev.sbutler.bitflask.server;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.util.concurrent.MoreExecutors.shutdownAndAwaitTermination;

import com.google.common.collect.ImmutableSet;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.common.util.concurrent.ServiceManager.Listener;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.common.concurrency.VirtualThreadConcurrencyModule;
import dev.sbutler.bitflask.common.guice.RootModule;
import dev.sbutler.bitflask.config.BitflaskConfig;
import dev.sbutler.bitflask.config.ConfigLoader;
import dev.sbutler.bitflask.config.ConfigModule;
import dev.sbutler.bitflask.config.ServerConfig;
import dev.sbutler.bitflask.server.configuration.ServerModule;
import dev.sbutler.bitflask.storage.StorageServiceModule;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.time.Duration;
import java.time.Instant;
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

  private Server() {}

  public static void main(String[] args) {
    try {
      executionStarted = Instant.now();
      BitflaskConfig bitflaskConfig = ConfigLoader.load();
      printConfigInfo(bitflaskConfig);

      ServerSocketChannel serverSocketChannel =
          createServerSocketChannel(bitflaskConfig.getServerConfig());

      ImmutableSet<RootModule> rootModules =
          ImmutableSet.of(new ServerModule(serverSocketChannel), new StorageServiceModule());

      Injector injector =
          Guice.createInjector(
              ImmutableSet.<AbstractModule>builder()
                  .add(new ConfigModule(bitflaskConfig))
                  .add(new VirtualThreadConcurrencyModule())
                  .addAll(rootModules)
                  .build());

      ImmutableSet<Service> services =
          rootModules.stream()
              .flatMap(module -> module.getServices(injector).stream())
              .collect(toImmutableSet());

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

  private static ServerSocketChannel createServerSocketChannel(ServerConfig serverConfig)
      throws IOException {
    ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
    ServerConfig.ServerInfo thisServerInfo =
        serverConfig.getBitflaskServersMap().get(serverConfig.getThisServerId());
    InetSocketAddress inetSocketAddress = new InetSocketAddress(thisServerInfo.getRespPort());
    serverSocketChannel.bind(inetSocketAddress);
    return serverSocketChannel;
  }

  private static void addServiceManagerListener(
      ServiceManager serviceManager, ListeningExecutorService listeningExecutorService) {
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
            logger.atSevere().withCause(service.failureCause()).log(
                "[%s] failed.", service.getClass());
            serviceManager.stopAsync();
          }
        },
        listeningExecutorService);
  }

  private static void registerShutdownHook(
      ServiceManager serviceManager, ListeningExecutorService listeningExecutorService) {
    Runtime.getRuntime()
        .addShutdownHook(
            Thread.ofVirtual()
                .unstarted(
                    () -> {
                      System.out.println("Starting Server shutdown hook");
                      // Give the services 5 seconds to stop to ensure that we are responsive to
                      // shut down requests.
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

  private static void printConfigInfo(BitflaskConfig bitflaskConfig) {
    logger.atInfo().log("Using java version [%s]", System.getProperty("java.version"));
    logger.atInfo().log(
        "Runtime processors available [%d]", Runtime.getRuntime().availableProcessors());

    logger.atInfo().log(bitflaskConfig.toString());
  }

  private static void logTimeFromStart() {
    Instant now = Instant.now();
    Duration startupTime = Duration.between(executionStarted, now);
    logger.atInfo().log("Time to startup: [%d]millis", startupTime.toMillis());
  }
}
