package dev.sbutler.bitflask.client.connection;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ConnectionModule extends AbstractModule {

  private final ConnectionManager connectionManager;

  public ConnectionModule() throws IOException {
    this.connectionManager = new LocalhostConnectionManager();
  }

  @Provides
  ConnectionManager provideConnectionManager() {
    return this.connectionManager;
  }

  @Provides
  InputStream provideInputStream(ConnectionManager connectionManager) {
    return connectionManager.getInputStream();
  }

  @Provides
  OutputStream provideOutputStream(ConnectionManager connectionManager) {
    return connectionManager.getOutputStream();
  }

}
