package dev.sbutler.bitflask.server.configuration;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.sbutler.bitflask.server.configuration.concurrency.ConcurrencyModule;
import dev.sbutler.bitflask.server.network_service.NetworkServiceModule;
import dev.sbutler.bitflask.storage.StorageServiceModule;

public class ServerModule extends AbstractModule {

  private final ServerConfiguration serverConfiguration;

  public ServerModule(ServerConfiguration serverConfiguration) {
    this.serverConfiguration = serverConfiguration;
  }

  @Override
  protected void configure() {
    super.configure();
    install(ConcurrencyModule.getInstance());
    install(StorageServiceModule.getInstance());
    install(new NetworkServiceModule());
  }

  @Provides
  @ServerPort
  int provideServerPort() {
    return serverConfiguration.getPort();
  }
}
