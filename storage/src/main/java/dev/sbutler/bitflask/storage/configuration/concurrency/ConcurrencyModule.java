package dev.sbutler.bitflask.storage.configuration.concurrency;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import java.util.concurrent.Executors;
import javax.inject.Singleton;

public class ConcurrencyModule extends AbstractModule {

  private static final ConcurrencyModule instance = new ConcurrencyModule();

  private ListeningExecutorService executorService = null;

  private ConcurrencyModule() {
  }

  public static ConcurrencyModule getInstance() {
    return instance;
  }

  @Provides
  @StorageExecutorService
  @Singleton
  ListeningExecutorService provideExecutorService(StorageThreadFactory storageThreadFactory) {
    if (executorService == null) {
      executorService = MoreExecutors.listeningDecorator(
          Executors.newThreadPerTaskExecutor(storageThreadFactory));
    }
    return executorService;
  }

}
