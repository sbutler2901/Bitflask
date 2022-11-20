package dev.sbutler.bitflask.client.network;

import com.google.inject.AbstractModule;
import com.google.inject.throwingproviders.ThrowingProviderBinder;
import dev.sbutler.bitflask.resp.network.RespService;
import java.nio.channels.SocketChannel;
import javax.inject.Singleton;

public class NetworkModule extends AbstractModule {

  @Override
  protected void configure() {
    ThrowingProviderBinder.create(binder())
        .bind(SocketChannelProvider.class, SocketChannel.class)
        .to(SocketChannelProviderImpl.class)
        .in(Singleton.class);
    ThrowingProviderBinder.create(binder())
        .bind(RespServiceProvider.class, RespService.class)
        .to(RespServiceProviderImpl.class)
        .in(Singleton.class);
//    install(ThrowingProviderBinder.forModule(this));
  }

//  @CheckedProvides(SocketChannelProvider.class)
//  @Singleton
//  SocketChannel provideSocketChannel(SocketChannelProviderImpl socketChannelProvider)
//      throws IOException {
//    return socketChannelProvider.get();
//  }
//
//  @CheckedProvides(RespServiceProvider.class)
//  RespService provideRespService(RespServiceProviderImpl respServiceProvider) throws IOException {
//    return respServiceProvider.get();
//  }
}
