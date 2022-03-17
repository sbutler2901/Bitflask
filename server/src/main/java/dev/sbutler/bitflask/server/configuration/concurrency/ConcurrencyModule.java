package dev.sbutler.bitflask.server.configuration.concurrency;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.sbutler.bitflask.server.configuration.ServerNumThreads;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javax.inject.Singleton;

public class ConcurrencyModule extends AbstractModule {

  private static final ConcurrencyModule instance = new ConcurrencyModule();

  private ExecutorService executorService;

  private ConcurrencyModule() {
  }

  public static ConcurrencyModule getInstance() {
    return instance;
  }

  @Provides
  @Singleton
  ThreadFactory provideThreadFactory() {
    return new ServerThreadFactory();
  }

  @Provides
  @Singleton
  ExecutorService provideExecutorService(@ServerNumThreads int numThreads,
      ThreadFactory threadFactory) {
    if (executorService == null) {
      executorService = Executors.newFixedThreadPool(numThreads, threadFactory);
    }
    return executorService;
  }

}
