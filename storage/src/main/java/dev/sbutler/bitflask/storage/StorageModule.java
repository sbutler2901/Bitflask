package dev.sbutler.bitflask.storage;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StorageModule extends AbstractModule {

  private static final StorageModule instance = new StorageModule();

  private ExecutorService executorService;
  private Storage storage;

  private StorageModule() {
  }

  public static StorageModule getInstance() {
    return instance;
  }

  @Provides
  @StorageNumThreads
  int provideStorageNumThreads() {
    return 4;
  }

  @Provides
  @Singleton
  @StorageExecutorService
  ExecutorService provideExecutorService(@StorageNumThreads int numThreads) {
    if (executorService == null) {
      executorService = Executors.newFixedThreadPool(numThreads);
    }
    return executorService;
  }

  @Provides
  @Singleton
  Storage provideStorage(@StorageExecutorService ExecutorService executorService)
      throws IOException {
    if (storage == null) {
      storage = new StorageImpl(executorService);
    }
    return storage;
  }

}
