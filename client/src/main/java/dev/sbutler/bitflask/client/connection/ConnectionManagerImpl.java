package dev.sbutler.bitflask.client.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import javax.inject.Inject;

public class ConnectionManagerImpl implements ConnectionManager {

  private final Socket socket;

  @Inject
  public ConnectionManagerImpl(@ServerHost String serverHost, @ServerPort int serverPort)
      throws IOException {
    this.socket = new Socket(serverHost, serverPort);
  }

  @Override
  public void close() throws IOException {
    socket.close();
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return socket.getInputStream();
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    return socket.getOutputStream();
  }
}
