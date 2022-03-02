package dev.sbutler.bitflask.server.client_connection;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ClientConnectionModule extends AbstractModule {

  private final Socket socket;

  public ClientConnectionModule(Socket socket) {
    this.socket = socket;
  }

  @Provides
  Socket provideSocket() {
    return socket;
  }

  @Provides
  ClientConnectionManager provideClientConnectionManager(
      ClientConnectionManagerImpl clientConnectionManager) {
    return clientConnectionManager;
  }

  @Provides
  InputStream provideInputStream(ClientConnectionManager connectionManager) throws IOException {
    return connectionManager.getInputStream();
  }

  @Provides
  OutputStream provideOutputStream(ClientConnectionManager connectionManager) throws IOException {
    return connectionManager.getOutputStream();
  }
}
