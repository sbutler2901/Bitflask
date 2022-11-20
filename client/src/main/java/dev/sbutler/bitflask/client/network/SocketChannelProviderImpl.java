package dev.sbutler.bitflask.client.network;

import dev.sbutler.bitflask.client.configuration.ClientConfigurations;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import javax.inject.Inject;

public class SocketChannelProviderImpl implements SocketChannelProvider {

  private final ClientConfigurations configurations;

  @Inject
  public SocketChannelProviderImpl(ClientConfigurations configurations) {
    this.configurations = configurations;
  }

  @Override
  public SocketChannel get() throws IOException {
    SocketAddress socketAddress =
        new InetSocketAddress(configurations.getHost(), configurations.getPort());
    return SocketChannel.open(socketAddress);
  }
}
