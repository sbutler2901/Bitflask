package dev.sbutler.bitflask.client;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.sbutler.bitflask.client.client_processing.ClientProcessingModule;
import dev.sbutler.bitflask.client.configuration.ClientConfiguration;
import dev.sbutler.bitflask.resp.network.RespService;

public class ClientModule extends AbstractModule {

  public static ClientModule create(ClientConfiguration configuration,
      RespService respService) {
    return new ClientModule(configuration, respService);
  }

  private final ClientConfiguration configuration;
  private final RespService respService;

  private ClientModule(ClientConfiguration configuration, RespService respService) {
    this.configuration = configuration;
    this.respService = respService;
  }

  @Override
  protected void configure() {
    install(new ClientProcessingModule());
  }

  @Provides
  ClientConfiguration provideClientConfiguration() {
    return configuration;
  }

  @Provides
  RespService provideRespService() {
    return respService;
  }
}
