package dev.sbutler.bitflask.server.configuration;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import dev.sbutler.bitflask.storage.Storage;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ServerModule extends AbstractModule {

  private static final ServerModule instance = new ServerModule();

  private ThreadPoolExecutor threadPoolExecutor;
  private Storage storage;

  private ServerModule() {}

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
  ThreadPoolExecutor provideThreadPoolExecutor(@ServerNumThreads int numThreads) {
    if (threadPoolExecutor == null) {
      threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(numThreads);
    }
    return threadPoolExecutor;
  }

  @Provides
  @Singleton
  Storage provideStorage(ThreadPoolExecutor threadPoolExecutor) throws IOException {
    if (storage == null) {
      storage = new Storage(threadPoolExecutor);
    }
    return storage;
  }

  @Provides
  @Singleton
  ServerSocket provideServerSocket(@ServerPort int port) throws IOException {
    return new ServerSocket(port);
  }
}
