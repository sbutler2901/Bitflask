package dev.sbutler.bitflask.client.network;

import com.google.inject.AbstractModule;
import dev.sbutler.bitflask.resp.network.RespService;
import java.nio.channels.SocketChannel;
import javax.inject.Singleton;

public class NetworkModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(SocketChannel.class)
        .toProvider(SocketChannelProvider.class)
        .in(Singleton.class);
    bind(RespService.class)
        .toProvider(RespServiceProvider.class)
        .in(Singleton.class);
  }
}
