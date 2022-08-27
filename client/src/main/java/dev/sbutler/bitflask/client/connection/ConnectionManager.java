package dev.sbutler.bitflask.client.connection;

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
  public ConnectionManager(@ServerHost String serverHost, @ServerPort int serverPort)
      throws IOException {
    this.socket = new Socket(serverHost, serverPort);
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
