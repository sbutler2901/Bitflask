package dev.sbutler.bitflask.storage.configuration.concurrency;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
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
  @StorageThreadFactory
  @Singleton
  ThreadFactory provideStorageThreadFactory() {
    return new StorageThreadFactoryImpl();
  }

  @Provides
  @StorageNumThreads
  int provideStorageNumThreads() {
    return 4;
  }

  @Provides
  @StorageExecutorService
  @Singleton
  ExecutorService provideExecutorService(@StorageNumThreads int numThreads,
      @StorageThreadFactory ThreadFactory threadFactory) {
    if (executorService == null) {
      executorService = Executors.newFixedThreadPool(numThreads, threadFactory);
    }
    return executorService;
  }

}
