package dev.sbutler.bitflask.server.configuration.concurrency;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
  ExecutorService provideExecutorService(ServerThreadFactory serverThreadFactory) {
    if (executorService == null) {
      executorService = Executors.newThreadPerTaskExecutor(serverThreadFactory);
    }
    return executorService;
  }
}
