package dev.sbutler.bitflask.client;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.sbutler.bitflask.client.client_processing.ClientProcessingModule;
import dev.sbutler.bitflask.client.configuration.ClientConfiguration;
import dev.sbutler.bitflask.resp.network.RespReader;
import dev.sbutler.bitflask.resp.network.RespWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import javax.inject.Singleton;

public class ClientModule extends AbstractModule {

  public static ClientModule create(ClientConfiguration configuration,
      ConnectionManager connectionManager) {
    return new ClientModule(configuration, connectionManager);
  }

  private final ClientConfiguration configuration;
  private final ConnectionManager connectionManager;

  private ClientModule(ClientConfiguration configuration, ConnectionManager connectionManager) {
    this.configuration = configuration;
    this.connectionManager = connectionManager;
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
  @Singleton
  RespReader provideRespReader() throws IOException {
    return new RespReader(new InputStreamReader(connectionManager.getInputStream()));
  }

  @Provides
  @Singleton
  RespWriter provideRespWriter() throws IOException {
    return new RespWriter(connectionManager.getOutputStream());
  }
}
