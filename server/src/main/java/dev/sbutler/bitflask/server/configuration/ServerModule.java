package dev.sbutler.bitflask.server.configuration;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.sbutler.bitflask.server.configuration.concurrency.ConcurrencyModule;
import dev.sbutler.bitflask.storage.StorageServiceModule;

public class ServerModule extends AbstractModule {

  private static final ServerModule instance = new ServerModule();
  private static ServerConfigurations serverConfigurations = null;

  private ServerModule() {
  }

  public static void setServerConfiguration(ServerConfigurations serverConfigurations) {
    ServerModule.serverConfigurations = serverConfigurations;
  }

  public static ServerModule getInstance() {
    if (serverConfigurations == null) {
      throw new IllegalStateException("ServerConfigurations must be set before using this module");
    }
    return instance;
  }

  @Override
  protected void configure() {
    super.configure();
    install(ConcurrencyModule.getInstance());
    install(StorageServiceModule.getInstance());
  }

  @Provides
  ServerConfigurations provideServerConfiguration() {
    return serverConfigurations;
  }
}
