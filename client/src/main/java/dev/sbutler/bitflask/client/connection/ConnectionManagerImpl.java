package dev.sbutler.bitflask.client.connection;

import com.google.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ConnectionManagerImpl implements ConnectionManager {

  private final Socket socket;

  @Inject
  public ConnectionManagerImpl(Socket socket) {
    this.socket = socket;
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
