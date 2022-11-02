package dev.sbutler.bitflask.client;

import com.beust.jcommander.JCommander;
import com.google.common.base.Joiner;
import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.client.client_processing.ClientProcessorService;
import dev.sbutler.bitflask.client.client_processing.ReplClientProcessorService;
import dev.sbutler.bitflask.client.client_processing.repl.ReplReader;
import dev.sbutler.bitflask.client.configuration.ClientConfiguration;
import dev.sbutler.bitflask.client.configuration.ClientConfigurationConstants;
import dev.sbutler.bitflask.common.configuration.ConfigurationDefaultProvider;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ResourceBundle;

public class Client implements Runnable {

  private final ClientConfiguration configuration;
  private final ConnectionManager connectionManager;

  Client(ClientConfiguration configuration, ConnectionManager connectionManager) {
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
    SocketAddress socketAddress = new InetSocketAddress(configuration.getHost(),
        configuration.getPort());
    SocketChannel socketChannel = SocketChannel.open(socketAddress);
    return new ConnectionManager(socketChannel);
  }

  @Override
  public void run() {
    Injector injector = Guice.createInjector(ClientModule.create(configuration, connectionManager));
    if (shouldExecuteWithRepl()) {
      Reader reader = new InputStreamReader(System.in);
      execute(injector, reader);
    } else {
      String inlineCmd = Joiner.on(' ').join(configuration.getInlineCmd());
      Reader reader = new StringReader(inlineCmd);
      execute(injector, reader);
    }
  }

  private void execute(Injector injector, Reader reader) {
    ReplReader replReader = new ReplReader(reader);
    ReplClientProcessorService.Factory replFactory =
        injector.getInstance(ReplClientProcessorService.Factory.class);
    ClientProcessorService clientProcessorService =
        replFactory.create(replReader);

    startClientProcessing(clientProcessorService);
  }

  private void startClientProcessing(ClientProcessorService clientProcessorService) {
    registerShutdownHook(clientProcessorService);
    clientProcessorService.run();
  }

  private boolean shouldExecuteWithRepl() {
    return configuration.getInlineCmd().size() == 0;
  }

  private void registerShutdownHook(ClientProcessorService clientProcessorService) {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("Exiting...");
      clientProcessorService.triggerShutdown();
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
