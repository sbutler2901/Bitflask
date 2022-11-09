package dev.sbutler.bitflask.resp.network;

import dev.sbutler.bitflask.resp.types.RespElement;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.channels.SocketChannel;

public class RespService implements AutoCloseable {

  public static class Factory {

    private final SocketChannel socketChannel;

    public Factory(SocketChannel socketChannel) {
      this.socketChannel = socketChannel;
    }

    public RespService create() throws IOException {
      Reader socketReader = new InputStreamReader(socketChannel.socket().getInputStream());
      RespReader respReader = new RespReader(socketReader);
      RespWriter respWriter = new RespWriter(socketChannel.socket().getOutputStream());
      return new RespService(socketChannel, respReader, respWriter);
    }
  }

  private final SocketChannel socketChannel;
  private final RespReader respReader;
  private final RespWriter respWriter;

  private RespService(SocketChannel socketChannel, RespReader respReader, RespWriter respWriter) {
    this.socketChannel = socketChannel;
    this.respReader = respReader;
    this.respWriter = respWriter;
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
