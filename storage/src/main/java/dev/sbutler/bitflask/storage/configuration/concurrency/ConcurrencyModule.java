package dev.sbutler.bitflask.storage.configuration.concurrency;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javax.inject.Singleton;

public class ConcurrencyModule extends AbstractModule {

  private static final String STORAGE_SERVICE_THREAD_NAME = "storage-pool-%d";

  private static final ConcurrencyModule instance = new ConcurrencyModule();

  private ListeningExecutorService executorService = null;

  private ConcurrencyModule() {
  }

  public static ConcurrencyModule getInstance() {
    return instance;
  }

  @Provides
  @StorageNumThreads
  int provideStorageNumThreads() {
    return 4;
  }

  @Provides
  @StorageThreadFactory
  @Singleton
  ThreadFactory provideStorageThreadFactory() {
    return new ThreadFactoryBuilder().setNameFormat(STORAGE_SERVICE_THREAD_NAME).build();
  }

  @Provides
  @StorageExecutorService
  @Singleton
  ListeningExecutorService provideExecutorService(@StorageNumThreads int numThreads,
      @StorageThreadFactory ThreadFactory threadFactory) {
    if (executorService == null) {
      executorService = MoreExecutors.listeningDecorator(
          Executors.newFixedThreadPool(numThreads, threadFactory));
    }
    return executorService;
  }

}
