package dev.sbutler.bitflask.client.connection;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.inject.Singleton;

public class ConnectionModule extends AbstractModule {

  @Provides
  @ServerHost
  static String provideServerHost() {
    return "localhost";
  }

  @Provides
  @ServerPort
  static int provideServerPort() {
    return 9090;
  }

  @Provides
  @Singleton
  ConnectionManager provideConnectionManager(ConnectionManager connectionManager) {
    return connectionManager;
  }

  @Provides
  InputStream provideInputStream(ConnectionManager connectionManager) throws IOException {
    return connectionManager.getInputStream();
  }

  @Provides
  OutputStream provideOutputStream(ConnectionManager connectionManager) throws IOException {
    return connectionManager.getOutputStream();
  }

}
