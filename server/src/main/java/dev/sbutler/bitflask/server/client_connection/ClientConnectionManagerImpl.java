package dev.sbutler.bitflask.server.client_connection;

import com.google.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ClientConnectionManagerImpl implements ClientConnectionManager {

  private final Socket socket;

  @Inject
  ClientConnectionManagerImpl(Socket socket) {
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
