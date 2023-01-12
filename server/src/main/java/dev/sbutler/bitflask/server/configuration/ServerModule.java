package dev.sbutler.bitflask.server.configuration;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class ServerModule extends AbstractModule {

  private final ServerConfigurations serverConfigurations;

  public ServerModule(ServerConfigurations serverConfigurations) {
    this.serverConfigurations = serverConfigurations;
  }

  @Provides
  ServerConfigurations provideServerConfiguration() {
    return serverConfigurations;
  }
}
