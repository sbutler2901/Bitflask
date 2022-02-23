package dev.sbutler.bitflask.client.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class LocalhostConnectionManager implements ConnectionManager {

  private static final String SERVER_HOST = "localhost";
  private static final int SERVER_PORT = 9090;

  private final Socket socket;
  private final InputStream inputStream;
  private final OutputStream outputStream;

  public LocalhostConnectionManager() throws IOException {
    this.socket = new Socket(SERVER_HOST, SERVER_PORT);
    this.inputStream = socket.getInputStream();
    this.outputStream = socket.getOutputStream();
  }

  @Override
  public void close() throws IOException {
    socket.close();
  }

  @Override
  public InputStream getInputStream() {
    return inputStream;
  }

  @Override
  public OutputStream getOutputStream() {
    return outputStream;
  }
}
