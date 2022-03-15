package dev.sbutler.bitflask.server.client_handling.connection;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ClientConnectionModule extends AbstractModule {

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
