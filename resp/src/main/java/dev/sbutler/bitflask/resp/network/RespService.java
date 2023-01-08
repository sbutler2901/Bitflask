package dev.sbutler.bitflask.resp.network;

import dev.sbutler.bitflask.resp.types.RespElement;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.channels.SocketChannel;

/**
 * Handles reading and writing {@link RespElement}s via a {@link SocketChannel}.
 *
 * <p>This class is expected to assume ownership of the provided SocketChannel with the service's
 * lifecycle tied to it. Closing this service closes the underlying SocketChannel.
 */
public final class RespService implements AutoCloseable {

  private final SocketChannel socketChannel;
  private final RespReader respReader;
  private final RespWriter respWriter;

  private RespService(SocketChannel socketChannel, RespReader respReader, RespWriter respWriter) {
    this.socketChannel = socketChannel;
    this.respReader = respReader;
    this.respWriter = respWriter;
  }

  public static RespService create(SocketChannel socketChannel) throws IOException {
    Reader socketReader = new InputStreamReader(socketChannel.socket().getInputStream());
    RespReader respReader = new RespReader(socketReader);
    RespWriter respWriter = new RespWriter(socketChannel.socket().getOutputStream());
    return new RespService(socketChannel, respReader, respWriter);
  }

  public RespElement read() throws IOException {
    return respReader.readNextRespElement();
  }

  public void write(RespElement respElement) throws IOException {
    respWriter.writeRespElement(respElement);
  }

  @Override
  public void close() throws IOException {
    socketChannel.close();
  }
}
