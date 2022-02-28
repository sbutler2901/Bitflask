package dev.sbutler.bitflask.server.configuration;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import dev.sbutler.bitflask.server.storage.Storage;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ServerModule extends AbstractModule {

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
  ThreadPoolExecutor provideThreadPoolExecutor(@ServerNumThreads int numThreads) {
    return (ThreadPoolExecutor) Executors.newFixedThreadPool(numThreads);
  }

  @Provides
  @Singleton
  Storage provideStorage(ThreadPoolExecutor threadPoolExecutor) throws IOException {
    return new Storage(threadPoolExecutor);
  }

  @Provides
  @Singleton
  ServerSocket provideServerSocket(@ServerPort int port) throws IOException {
    return new ServerSocket(port);
  }
}
