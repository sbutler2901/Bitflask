package dev.sbutler.bitflask.client.network;

import com.google.inject.ProvisionException;
import dev.sbutler.bitflask.resp.network.RespService;
import dev.sbutler.bitflask.resp.network.RespService.Factory;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import javax.inject.Inject;
import javax.inject.Provider;

class RespServiceProvider implements Provider<RespService> {

  private final RespService.Factory factory;

  @Inject
  RespServiceProvider(SocketChannel socketChannel) {
    this.factory = new Factory(socketChannel);
  }

  @Override
  public RespService get() {
    try {
      return factory.create();
    } catch (IOException e) {
      throw new ProvisionException(
          String.format("Failed to provision RespService: %s", e.getMessage()));
    }
  }
}
