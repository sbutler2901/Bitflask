package dev.sbutler.bitflask.server.network_service.client_handling_service;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.sbutler.bitflask.server.network_service.client_handling_service.connection.ClientConnectionModule;
import dev.sbutler.bitflask.server.network_service.client_handling_service.processing.ClientProcessingModule;
import dev.sbutler.bitflask.storage.StorageCommandDispatcher;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;

public class ClientHandlingServiceModule extends AbstractModule {

  private final ExecutorService executorService;
  private final SocketChannel socketChannel;
  private final StorageCommandDispatcher storageCommandDispatcher;

  public ClientHandlingServiceModule(ExecutorService executorService, SocketChannel socketChannel,
      StorageCommandDispatcher storageCommandDispatcher) {
    this.executorService = executorService;
    this.socketChannel = socketChannel;
    this.storageCommandDispatcher = storageCommandDispatcher;
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
  ExecutorService provideExecutorService() {
    return executorService;
  }

  @Provides
  StorageCommandDispatcher provideStorageCommandDispatcher() {
    return storageCommandDispatcher;
  }
}
