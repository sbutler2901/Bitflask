package dev.sbutler.bitflask.server.network_service;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.sbutler.bitflask.server.configuration.ServerPort;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import javax.inject.Singleton;

public class NetworkServiceModule extends AbstractModule {

  @Provides
  @Singleton
  NetworkServiceImpl provideNetworkService(NetworkServiceImpl networkService) {
    return networkService;
  }

  @Provides
  @Singleton
  ServerSocketChannel provideServerSocketChannel(@ServerPort int port) throws IOException {
    ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
    InetSocketAddress inetSocketAddress = new InetSocketAddress(port);
    serverSocketChannel.bind(inetSocketAddress);
    return serverSocketChannel;
  }
}
