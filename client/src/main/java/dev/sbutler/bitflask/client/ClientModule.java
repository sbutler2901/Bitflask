package dev.sbutler.bitflask.client;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.sbutler.bitflask.client.client_processing.ClientProcessingModule;
import dev.sbutler.bitflask.client.configuration.ClientConfigurations;
import dev.sbutler.bitflask.resp.network.RespService;

public class ClientModule extends AbstractModule {

  public static ClientModule create(ClientConfigurations configuration,
      RespService respService) {
    return new ClientModule(configuration, respService);
  }

  private final ClientConfigurations configuration;
  private final RespService respService;

  private ClientModule(ClientConfigurations configuration, RespService respService) {
    this.configuration = configuration;
    this.respService = respService;
  }

  @Override
  protected void configure() {
    install(new ClientProcessingModule());
  }

  @Provides
  ClientConfigurations provideClientConfiguration() {
    return configuration;
  }

  @Provides
  RespService provideRespService() {
    return respService;
  }
}
