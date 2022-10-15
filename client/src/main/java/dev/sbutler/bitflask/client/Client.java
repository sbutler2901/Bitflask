package dev.sbutler.bitflask.client;

import com.beust.jcommander.JCommander;
import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.client.client_processing.ClientProcessor;
import dev.sbutler.bitflask.client.client_processing.ReplClientProcessorService;
import dev.sbutler.bitflask.client.configuration.ClientConfiguration;
import dev.sbutler.bitflask.client.configuration.ClientConfigurationConstants;
import dev.sbutler.bitflask.common.configuration.ConfigurationDefaultProvider;
import java.io.IOException;
import java.net.Socket;
import java.util.ResourceBundle;

public class Client implements Runnable {

  private final ClientConfiguration configuration;
  private final ConnectionManager connectionManager;

  private Client(ClientConfiguration configuration, ConnectionManager connectionManager) {
    this.configuration = configuration;
    this.connectionManager = connectionManager;
  }

  public static void main(String[] args) {
    ClientConfiguration configuration = initializeConfiguration(args);
    try {
      ConnectionManager connectionManager = createConnectionManager(configuration);
      Client client = new Client(configuration, connectionManager);
      client.run();
    } catch (IOException e) {
      System.err.println("Failed to initialize connection to the server" + e);
      System.exit(1);
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
        .build()
        .parse(args);
    return configuration;
  }

  private static ConnectionManager createConnectionManager(ClientConfiguration configuration)
      throws IOException {
    Socket serverSocket = new Socket(configuration.getHost(), configuration.getPort());
    return new ConnectionManager(serverSocket);
  }

  @Override
  public void run() {
    Injector injector = Guice.createInjector(new ClientModule(configuration, connectionManager));
    if (shouldExecuteWithRepl()) {
      executeWithRepl(injector);
    } else {
      executeInline(injector);
    }
  }

  private void executeWithRepl(Injector injector) {
    ReplClientProcessorService replClientProcessorService =
        injector.getInstance(ReplClientProcessorService.class);
    registerShutdownHook(replClientProcessorService);
    replClientProcessorService.run();
  }

  private void executeInline(Injector injector) {
    registerShutdownHook();
    ClientProcessor clientProcessor = injector.getInstance(ClientProcessor.class);
    ImmutableList<String> clientInput = ImmutableList.copyOf(configuration.getInlineCmd());
    clientProcessor.processClientInput(clientInput);
  }

  private boolean shouldExecuteWithRepl() {
    return configuration.getInlineCmd().size() == 0;
  }

  private void registerShutdownHook() {
    Runtime.getRuntime().addShutdownHook(new Thread(this::closeConnectionManager));
  }

  private void registerShutdownHook(ReplClientProcessorService replClientProcessorService) {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("Exiting...");
      replClientProcessorService.triggerShutdown();
      closeConnectionManager();
    }));
  }

  private void closeConnectionManager() {
    try {
      connectionManager.close();
    } catch (IOException e) {
      System.err.println("Issues closing connection " + e);
    }
  }
}
