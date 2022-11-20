package dev.sbutler.bitflask.client.network;

import dev.sbutler.bitflask.resp.network.RespService;
import dev.sbutler.bitflask.resp.network.RespService.Factory;
import java.io.IOException;
import javax.inject.Inject;

public class RespServiceProviderImpl implements RespServiceProvider {

  //  private final SocketChannel socketChannel;
  private final SocketChannelProvider socketChannelProvider;

  @Inject
//  public RespServiceProviderImpl(SocketChannel socketChannel) {
  public RespServiceProviderImpl(SocketChannelProvider socketChannelProvider) {
//    this.socketChannel = socketChannel;
    this.socketChannelProvider = socketChannelProvider;
  }

  @Override
  public RespService get() throws IOException {
    RespService.Factory factory = new Factory(socketChannelProvider.get());
    return factory.create();
  }
}
