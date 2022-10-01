package dev.sbutler.bitflask.server.network_service.client_handling_service;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.sbutler.bitflask.server.network_service.client_handling_service.connection.ClientConnectionModule;
import dev.sbutler.bitflask.server.network_service.client_handling_service.processing.ClientProcessingModule;
import java.nio.channels.SocketChannel;

public class ClientHandlingServiceChildModule extends AbstractModule {

  private final SocketChannel socketChannel;

  public ClientHandlingServiceChildModule(SocketChannel socketChannel) {
    this.socketChannel = socketChannel;
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
}
