package dev.sbutler.bitflask.server.configuration.concurrency;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javax.inject.Singleton;

public class ConcurrencyModule extends AbstractModule {

  private static final ConcurrencyModule instance = new ConcurrencyModule();

  private ListeningExecutorService listeningExecutorService;

  private ConcurrencyModule() {
  }

  public static ConcurrencyModule getInstance() {
    return instance;
  }

  @Provides
  @Singleton
  ListeningExecutorService provideExecutorService(ServerThreadFactory serverThreadFactory) {
    if (listeningExecutorService == null) {
      listeningExecutorService = MoreExecutors.listeningDecorator(
          Executors.newThreadPerTaskExecutor(serverThreadFactory));
    }
    return listeningExecutorService;
  }

  @Provides
  ThreadFactory provideThreadFactory(ServerThreadFactory serverThreadFactory) {
    return serverThreadFactory;
  }
}
