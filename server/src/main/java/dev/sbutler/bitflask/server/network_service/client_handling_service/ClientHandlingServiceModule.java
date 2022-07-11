package dev.sbutler.bitflask.server.network_service.client_handling_service;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.sbutler.bitflask.server.command_processing_service.ServerCommandDispatcher;
import dev.sbutler.bitflask.server.network_service.client_handling_service.connection.ClientConnectionModule;
import dev.sbutler.bitflask.server.network_service.client_handling_service.processing.ClientProcessingModule;
import java.nio.channels.SocketChannel;

public class ClientHandlingServiceModule extends AbstractModule {

  private final SocketChannel socketChannel;
  private final ServerCommandDispatcher serverCommandDispatcher;

  public ClientHandlingServiceModule(SocketChannel socketChannel,
      ServerCommandDispatcher serverCommandDispatcher) {
    this.socketChannel = socketChannel;
    this.serverCommandDispatcher = serverCommandDispatcher;
  }

  @Override
  protected void configure() {
    super.configure();
    install(new ClientConnectionModule());
    install(new ClientProcessingModule());
  }

  @Provides
  SocketChannel provideSocketChannel() {
    return socketChannel;
  }

  @Provides
  ServerCommandDispatcher provideServerCommandDispatcher() {
    return serverCommandDispatcher;
  }
}
