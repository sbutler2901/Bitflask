package dev.sbutler.bitflask.server.configuration;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import dev.sbutler.bitflask.server.network_service.NetworkService;
import dev.sbutler.bitflask.server.network_service.NetworkServiceImpl;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerModule extends AbstractModule {

  private static final ServerModule instance = new ServerModule();

  private ExecutorService executorService;
  private ServerSocketChannel serverSocketChannel;

  private ServerModule() {
  }

  public static ServerModule getInstance() {
    return instance;
  }

  @Provides
  @ServerPort
  int provideServerPort() {
    return 9090;
  }

  @Provides
  @ServerNumThreads
  int provideServerNumThreads() {
    return 4;
  }

  @Provides
  @Singleton
  ExecutorService provideExecutorService(@ServerNumThreads int numThreads) {
    if (executorService == null) {
      executorService = Executors.newFixedThreadPool(numThreads);
    }
    return executorService;
  }

  @Provides
  @Singleton
  ServerSocketChannel provideServerSocketChannel(@ServerPort int port) throws IOException {
    if (serverSocketChannel == null) {
      serverSocketChannel = ServerSocketChannel.open();
      InetSocketAddress inetSocketAddress = new InetSocketAddress(port);
      serverSocketChannel.bind(inetSocketAddress);
    }
    return serverSocketChannel;
  }

  // todo: determine best place for this
  @Provides
  @Singleton
  NetworkService provideNetworkService(NetworkServiceImpl networkService) {
    return networkService;
  }
}
