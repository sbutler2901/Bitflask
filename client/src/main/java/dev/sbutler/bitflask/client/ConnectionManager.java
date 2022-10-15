package dev.sbutler.bitflask.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
class ConnectionManager implements AutoCloseable {

  private final Socket socket;

  @Inject
  public ConnectionManager(Socket socket) {
    this.socket = socket;
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
