package dev.sbutler.bitflask.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Manages the {@link Socket} connected to a remote server.
 */
@Singleton
class ConnectionManager implements AutoCloseable {

  private final SocketChannel socketChannel;

  @Inject
  public ConnectionManager(SocketChannel socketChannel) {
    this.socketChannel = socketChannel;
  }

  @Override
  public void close() throws IOException {
    socketChannel.close();
  }

  public InputStream getInputStream() throws IOException {
    return socketChannel.socket().getInputStream();
  }

  public OutputStream getOutputStream() throws IOException {
    return socketChannel.socket().getOutputStream();
  }
}
