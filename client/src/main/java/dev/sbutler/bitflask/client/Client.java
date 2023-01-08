package dev.sbutler.bitflask.client;

import com.google.inject.AbstractModule;
import com.google.inject.ConfigurationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.ProvisionException;
import dev.sbutler.bitflask.client.client_processing.ReplClientProcessorService;
import dev.sbutler.bitflask.client.configuration.ClientConfigurations;
import dev.sbutler.bitflask.client.configuration.ClientConfigurationsConstants;
import dev.sbutler.bitflask.common.configuration.ConfigurationsBuilder;
import dev.sbutler.bitflask.resp.network.RespService;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ResourceBundle;

public class Client {

  public static void main(String[] args) {
    try {
      ClientConfigurations clientConfigurations = initializeConfiguration(args);
      SocketChannel socketChannel = createSocketChannel(clientConfigurations);
      RespService respService = RespService.create(socketChannel);

      ClientModule clientModule = new ClientModule.Builder()
          .addRuntimeModule(new AbstractModule() {
            @Override
            protected void configure() {
              super.configure();
              bind(RespService.class).toInstance(respService);
            }
          })
          .build();
      Injector injector = Guice.createInjector(clientModule);

      ReplClientProcessorService replClientProcessorService =
          injector.getInstance(ReplClientProcessorService.class);

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

  private static SocketChannel createSocketChannel(ClientConfigurations clientConfigurations)
      throws IOException {
    SocketAddress socketAddress = new InetSocketAddress(
        clientConfigurations.getHost(), clientConfigurations.getPort());
    return SocketChannel.open(socketAddress);
  }

  private static ClientConfigurations initializeConfiguration(String[] args) {
    ResourceBundle resourceBundle = ResourceBundle.getBundle("config");
    ConfigurationsBuilder configsBuilder = new ConfigurationsBuilder(args, resourceBundle);

    ClientConfigurations clientConfigurations = new ClientConfigurations();
    configsBuilder.build(clientConfigurations,
        ClientConfigurationsConstants.CLIENT_FLAG_TO_CONFIGURATION_MAP);
    return clientConfigurations;
  }

  private static void registerShutdownHook(
      ReplClientProcessorService replClientProcessorService,
      RespService respService) {
    Runtime.getRuntime().addShutdownHook(Thread.ofVirtual().unstarted(() -> {
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
