package dev.sbutler.bitflask.server.network_service.client_handling_service;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.sbutler.bitflask.resp.network.RespService;
import dev.sbutler.bitflask.server.command_processing_service.CommandProcessingService;
import java.nio.channels.SocketChannel;

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

  public static ClientHandlingServiceChildModule create(RespService respService) {
    return new ClientHandlingServiceChildModule(respService);
  }

  private final RespService respService;

  public ClientHandlingServiceChildModule(RespService respService) {
    this.respService = respService;
  }

  @Provides
  RespService provideRespService() {
    return respService;
  }
}
