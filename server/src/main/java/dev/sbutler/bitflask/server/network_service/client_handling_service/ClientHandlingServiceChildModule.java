package dev.sbutler.bitflask.server.network_service.client_handling_service;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.sbutler.bitflask.resp.network.RespReader;
import dev.sbutler.bitflask.resp.network.RespWriter;
import dev.sbutler.bitflask.server.command_processing_service.CommandProcessingService;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.SocketChannel;
import javax.inject.Singleton;

/**
 * Provides the necessary Guice bindings for classes initialized for a single client connection.
 *
 * <p>The {@link SocketChannel} for a specific client connection must be provided. The
 * configuration of all binds provided by an instance of this module will be based on this
 * connection.
 *
 * <p>This module is dependent on an externally provided binding for a
 * {@link CommandProcessingService} instance.
 */
public class ClientHandlingServiceChildModule extends AbstractModule {

  private final ClientConnectionManager connectionManager;

  public ClientHandlingServiceChildModule(ClientConnectionManager connectionManager) {
    this.connectionManager = connectionManager;
  }

  @Provides
  @Singleton
  ClientConnectionManager provideClientConnectionManager() {
    return connectionManager;
  }

  @Provides
  @Singleton
  ClientMessageProcessor provideClientMessageProcessor(
      CommandProcessingService commandProcessingService,
      RespReader respReader,
      RespWriter respWriter) {
    return new ClientMessageProcessor(commandProcessingService, respReader, respWriter);
  }

  @Provides
  @Singleton
  RespReader provideRespReader(ClientConnectionManager connectionManager) throws IOException {
    return new RespReader(new InputStreamReader(connectionManager.getInputStream()));
  }

  @Provides
  @Singleton
  RespWriter provideRespWriter(ClientConnectionManager connectionManager) throws IOException {
    return new RespWriter(connectionManager.getOutputStream());
  }
}
