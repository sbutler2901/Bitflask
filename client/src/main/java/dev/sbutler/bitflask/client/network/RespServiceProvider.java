package dev.sbutler.bitflask.client.network;

import com.google.inject.ProvisionException;
import dev.sbutler.bitflask.resp.network.RespService;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

class RespServiceProvider implements Provider<RespService> {

  private final SocketChannel socketChannel;

  @Inject
  RespServiceProvider(SocketChannel socketChannel) {
    this.socketChannel = socketChannel;
  }

  @Override
  @Singleton
  public RespService get() {
    try {
      return RespService.create(socketChannel);
    } catch (IOException e) {
      throw new ProvisionException("Failed to provision RespService", e);
    }
  }
}
