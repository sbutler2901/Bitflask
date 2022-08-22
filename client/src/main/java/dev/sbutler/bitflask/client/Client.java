package dev.sbutler.bitflask.client;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.common.util.concurrent.ServiceManager.Listener;
import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.client.client_processing.ClientProcessorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;

public class Client {

  public Client() {
  }

  public static void main(String[] args) {
    Injector injector = Guice.createInjector(new ClientModule());
    ImmutableSet<Service> services = ImmutableSet.of(
        injector.getInstance(ClientProcessorService.class)
    );
    ServiceManager serviceManager = new ServiceManager(services);
    addServiceManagerListener(serviceManager);
    registerShutdownHook(serviceManager);
    serviceManager.startAsync();
  }

  private static void addServiceManagerListener(ServiceManager serviceManager) {
    serviceManager.addListener(
        new Listener() {
          @Override
          public void failure(@Nonnull Service service) {
            System.err.printf("[%s] failed.", service.getClass());
            System.exit(1);
          }
        }, MoreExecutors.directExecutor());
  }

  private static void registerShutdownHook(ServiceManager serviceManager) {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("Exiting...");
      // Give the services 5 seconds to stop to ensure that we are responsive to shut down
      // requests.
      try {
        serviceManager.stopAsync().awaitStopped(500, TimeUnit.MILLISECONDS);
      } catch (TimeoutException timeout) {
        // stopping timed out
        System.err.println("ServiceManager timed out while stopping" + timeout);
      }
    }));
  }
}
