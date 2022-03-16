package dev.sbutler.bitflask.server.client_handling;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.sbutler.bitflask.server.client_handling.connection.ClientConnectionModule;
import dev.sbutler.bitflask.server.client_handling.processing.ClientProcessingModule;
import java.nio.channels.SocketChannel;
import javax.inject.Singleton;

public class ClientRequestModule extends AbstractModule {

  private final SocketChannel socketChannel;

  public ClientRequestModule(SocketChannel socketChannel) {
    this.socketChannel = socketChannel;
  }

  @Override
  protected void configure() {
    super.configure();
    install(new ClientConnectionModule());
    install(new ClientProcessingModule());
  }

  @Provides
  @Singleton
  ClientRequestHandler provideClientRequestHandler(ClientRequestHandlerImpl clientRequestHandler) {
    return clientRequestHandler;
  }

  @Provides
  SocketChannel provideSocketChannel() {
    return socketChannel;
  }

}
