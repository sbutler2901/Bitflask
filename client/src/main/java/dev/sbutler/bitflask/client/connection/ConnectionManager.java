package dev.sbutler.bitflask.client.connection;

import dev.sbutler.bitflask.client.configuration.ClientConfiguration;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ConnectionManager implements Closeable {

  private final Socket socket;

  @Inject
  public ConnectionManager(ClientConfiguration configuration) throws IOException {
    this.socket = new Socket(configuration.getHost(), configuration.getPort());
  }

  @Override
  public void close() throws IOException {
    socket.close();
  }

  public InputStream getInputStream() throws IOException {
    return socket.getInputStream();
  }

  public OutputStream getOutputStream() throws IOException {
    return socket.getOutputStream();
  }
}
