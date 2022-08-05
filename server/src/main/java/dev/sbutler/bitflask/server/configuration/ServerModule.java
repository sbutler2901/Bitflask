package dev.sbutler.bitflask.server.configuration;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.sbutler.bitflask.server.configuration.concurrency.ConcurrencyModule;
import dev.sbutler.bitflask.server.network_service.NetworkServiceModule;
import dev.sbutler.bitflask.storage.StorageServiceModule;

public class ServerModule extends AbstractModule {

  private static final ServerModule instance = new ServerModule();
  private static ServerConfiguration serverConfiguration = new ServerConfiguration();

  private ServerModule() {
  }

  public static void setServerConfiguration(ServerConfiguration serverConfiguration) {
    ServerModule.serverConfiguration = serverConfiguration;
  }

  public static ServerModule getInstance() {
    if (serverConfiguration == null) {
      throw new IllegalStateException(
          "The ServerModule must have the ServerConfiguration set before it can be used");
    }
    return instance;
  }

  @Override
  protected void configure() {
    super.configure();
    install(ConcurrencyModule.getInstance());
    install(StorageServiceModule.getInstance());
    install(new NetworkServiceModule());
  }

  @Provides
  ServerConfiguration provideServerConfiguration() {
    return serverConfiguration;
  }

  @Provides
  @ServerPort
  int provideServerPort() {
    return serverConfiguration.getPort();
  }
}
