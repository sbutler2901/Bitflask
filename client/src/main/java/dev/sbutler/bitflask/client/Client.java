package dev.sbutler.bitflask.client;

import com.google.inject.ConfigurationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.ProvisionException;
import dev.sbutler.bitflask.client.client_processing.ReplClientProcessorService;
import dev.sbutler.bitflask.client.configuration.ConfigurationModule;
import dev.sbutler.bitflask.resp.network.RespService;
import java.io.IOException;

public class Client {

  public static void main(String[] args) {
    try {
      ClientModule clientModule = new ClientModule.Builder()
          .addRuntimeModule(new ConfigurationModule(args))
          .build();
      Injector injector = Guice.createInjector(clientModule);

      ReplClientProcessorService replClientProcessorService =
          injector.getInstance(ReplClientProcessorService.class);
      RespService respService = injector.getInstance(RespService.class);

      registerShutdownHook(replClientProcessorService, respService);

      replClientProcessorService.run();
    } catch (ProvisionException e) {
      e.getErrorMessages().forEach(System.err::println);
    } catch (ConfigurationException e) {
      e.getErrorMessages().forEach(System.err::println);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void registerShutdownHook(
      ReplClientProcessorService replClientProcessorService,
      RespService respService) {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("Exiting...");
      replClientProcessorService.triggerShutdown();
      closeConnection(respService);
    }));
  }

  private static void closeConnection(RespService respService) {
    try {
      respService.close();
    } catch (IOException e) {
      System.err.println("Issues closing connection " + e);
    }
  }
}
