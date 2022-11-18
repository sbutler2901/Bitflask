package dev.sbutler.bitflask.client;

import com.google.common.base.Joiner;
import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.client.client_processing.ReplClientProcessorService;
import dev.sbutler.bitflask.client.client_processing.repl.ReplReader;
import dev.sbutler.bitflask.client.configuration.ClientConfigurations;
import dev.sbutler.bitflask.client.configuration.ClientConfigurationsConstants;
import dev.sbutler.bitflask.common.configuration.ConfigurationsBuilder;
import dev.sbutler.bitflask.resp.network.RespService;
import dev.sbutler.bitflask.resp.network.RespService.Factory;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ResourceBundle;

public class Client implements Runnable {

  private final Injector injector;
  private final ClientConfigurations configuration;

  Client(Injector injector, ClientConfigurations configuration) {
    this.injector = injector;
    this.configuration = configuration;
  }

  public static void main(String[] args) {
    ClientConfigurations configuration = initializeConfiguration(args);
    RespService respService;
    try {
      respService = createRespService(configuration);
    } catch (IOException e) {
      System.err.println("Failed to initialize connection to the server" + e);
      return;
    }

    Injector injector = Guice.createInjector(ClientModule.create(configuration, respService));
    Client client = new Client(injector, configuration);
    try {
      client.run();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static ClientConfigurations initializeConfiguration(String[] args) {
    ResourceBundle resourceBundle = ResourceBundle.getBundle("config");
    ConfigurationsBuilder configsBuilder = new ConfigurationsBuilder(args, resourceBundle);

    ClientConfigurations configuration = new ClientConfigurations();
    configsBuilder.build(configuration,
        ClientConfigurationsConstants.CLIENT_FLAG_TO_CONFIGURATION_MAP);

    return configuration;
  }

  private static RespService createRespService(ClientConfigurations configuration)
      throws IOException {
    SocketAddress socketAddress = new InetSocketAddress(configuration.getHost(),
        configuration.getPort());
    SocketChannel socketChannel = SocketChannel.open(socketAddress);
    RespService.Factory factory = new Factory(socketChannel);
    return factory.create();
  }

  @Override
  public void run() {
    ReplClientProcessorService replClientProcessorService =
        createReplClientProcessorService();
    registerShutdownHook(replClientProcessorService);
    replClientProcessorService.run();
  }

  private ReplClientProcessorService createReplClientProcessorService() {
    Reader userInputReader = createUserInputReader();
    ReplReader replReader = new ReplReader(userInputReader);
    ReplClientProcessorService.Factory replFactory =
        injector.getInstance(ReplClientProcessorService.Factory.class);
    return replFactory.create(replReader, shouldExecuteWithRepl());
  }

  private Reader createUserInputReader() {
    if (shouldExecuteWithRepl()) {
      return new InputStreamReader(System.in);
    }

    String inlineCmd = Joiner.on(' ').join(configuration.getInlineCmd());
    return new StringReader(inlineCmd);
  }

  private boolean shouldExecuteWithRepl() {
    return configuration.getInlineCmd().size() == 0;
  }

  private void registerShutdownHook(ReplClientProcessorService replClientProcessorService) {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("Exiting...");
      replClientProcessorService.triggerShutdown();
      closeConnection();
    }));
  }

  private void closeConnection() {
    try {
      injector.getInstance(RespService.class).close();
    } catch (IOException e) {
      System.err.println("Issues closing connection " + e);
    }
  }
}
