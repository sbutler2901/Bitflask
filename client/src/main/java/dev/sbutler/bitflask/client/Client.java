package dev.sbutler.bitflask.client;

import com.beust.jcommander.JCommander;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.common.util.concurrent.ServiceManager.Listener;
import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.client.client_processing.ClientProcessor;
import dev.sbutler.bitflask.client.client_processing.ReplClientProcessorService;
import dev.sbutler.bitflask.client.configuration.ClientConfiguration;
import dev.sbutler.bitflask.client.configuration.ClientConfigurationConstants;
import dev.sbutler.bitflask.client.connection.ConnectionManager;
import dev.sbutler.bitflask.common.configuration.ConfigurationDefaultProvider;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;

public class Client {

  private Client() {
  }

  public static void main(String[] args) {
    ClientConfiguration configuration = initializeConfiguration(args);
    Injector injector = Guice.createInjector(new ClientModule(configuration));
    if (shouldExecuteWithRepl(args)) {
      executeWithRepl(injector);
    } else {
      executeInline(injector, args);
    }
  }

  private static ClientConfiguration initializeConfiguration(String[] args) {
    ResourceBundle resourceBundle = ResourceBundle.getBundle("config");
    ClientConfiguration configuration = new ClientConfiguration();
    ConfigurationDefaultProvider defaultProvider =
        new ConfigurationDefaultProvider(
            ClientConfigurationConstants.CLIENT_FLAG_TO_CONFIGURATION_MAP,
            resourceBundle);
    JCommander.newBuilder()
        .addObject(configuration)
        .defaultProvider(defaultProvider)
        .acceptUnknownOptions(true)
        .build()
        .parse(args);
    return configuration;
  }

  private static void executeWithRepl(Injector injector) {
    ImmutableSet<Service> services = ImmutableSet.of(
        injector.getInstance(ReplClientProcessorService.class)
    );
    ServiceManager serviceManager = new ServiceManager(services);
    addServiceManagerListener(serviceManager);
    registerShutdownHook(injector.getInstance(ConnectionManager.class), serviceManager);
    serviceManager.startAsync();
  }

  private static void executeInline(Injector injector, String[] args) {
    registerShutdownHook(injector.getInstance(ConnectionManager.class));
    ClientProcessor clientProcessor = injector.getInstance(ClientProcessor.class);
    ImmutableList<String> clientInput = ImmutableList.copyOf(args);
    clientProcessor.processClientInput(clientInput);
  }

  private static boolean shouldExecuteWithRepl(String[] args) {
    return args.length == 0;
  }

  private static void addServiceManagerListener(ServiceManager serviceManager) {
    serviceManager.addListener(
        new Listener() {
          @Override
          public void failure(@Nonnull Service service) {
            System.err.printf("[%s] failed.", service.getClass());
          }
        }, MoreExecutors.directExecutor());
  }

  private static void registerShutdownHook(ConnectionManager connectionManager) {
    Runtime.getRuntime()
        .addShutdownHook(new Thread(() -> shutdownConnectionManager(connectionManager)));
  }

  private static void registerShutdownHook(ConnectionManager connectionManager,
      ServiceManager serviceManager) {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("Exiting...");
      shutdownServiceManager(serviceManager);
      shutdownConnectionManager(connectionManager);
    }));
  }

  private static void shutdownConnectionManager(ConnectionManager connectionManager) {
    try {
      connectionManager.close();
    } catch (IOException e) {
      System.err.println("Failure to close connection: " + e);
    }
  }

  private static void shutdownServiceManager(ServiceManager serviceManager) {
    // Give the services 5 seconds to stop to ensure that we are responsive to shut down
    // requests.
    try {
      serviceManager.stopAsync().awaitStopped(500, TimeUnit.MILLISECONDS);
    } catch (TimeoutException timeout) {
      // stopping timed out
      System.err.println("ServiceManager timed out while stopping" + timeout);
    }
  }
}
