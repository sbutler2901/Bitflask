package dev.sbutler.bitflask.server.network_service.client_handling_service.connection;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SocketChannel;
import javax.inject.Inject;

/**
 * Enables management of a specific client's connection resources.
 */
public class ClientConnectionManager implements Closeable {

  private final SocketChannel socketChannel;

  @Inject
  ClientConnectionManager(SocketChannel socketChannel) {
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
