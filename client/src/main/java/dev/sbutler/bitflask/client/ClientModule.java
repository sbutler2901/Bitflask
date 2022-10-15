package dev.sbutler.bitflask.client;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.sbutler.bitflask.client.client_processing.ClientProcessingModule;
import dev.sbutler.bitflask.client.command_processing.CommandProcessingModule;
import dev.sbutler.bitflask.client.configuration.ClientConfiguration;
import dev.sbutler.bitflask.client.connection.ConnectionModule;
import dev.sbutler.bitflask.resp.network.RespNetworkModule;

public class ClientModule extends AbstractModule {

  private final ClientConfiguration configuration;

  ClientModule(ClientConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  protected void configure() {
    install(new ConnectionModule());
    install(new RespNetworkModule());
    install(new CommandProcessingModule());
    install(new ClientProcessingModule());
  }

  @Provides
  ClientConfiguration provideClientConfiguration() {
    return configuration;
  }
}
