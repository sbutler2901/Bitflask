package dev.sbutler.bitflask.server.network_service.client_handling_service;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.sbutler.bitflask.server.configuration.concurrency.ConcurrencyModule;
import dev.sbutler.bitflask.server.network_service.client_handling_service.connection.ClientConnectionModule;
import dev.sbutler.bitflask.server.network_service.client_handling_service.processing.ClientProcessingModule;
import dev.sbutler.bitflask.storage.StorageServiceModule;
import java.nio.channels.SocketChannel;

public class ClientHandlingServiceModule extends AbstractModule {

  private final SocketChannel socketChannel;

  public ClientHandlingServiceModule(SocketChannel socketChannel) {
    this.socketChannel = socketChannel;
  }

  @Override
  protected void configure() {
    super.configure();
    install(ConcurrencyModule.getInstance());
    install(StorageServiceModule.getInstance());
    install(new ClientConnectionModule());
    install(new ClientProcessingModule());
  }

  @Provides
  SocketChannel provideSocketChannel() {
    return socketChannel;
  }
}
