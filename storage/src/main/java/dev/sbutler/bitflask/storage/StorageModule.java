package dev.sbutler.bitflask.storage;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class StorageModule extends AbstractModule {

  private static final StorageModule instance = new StorageModule();

  private ThreadPoolExecutor threadPoolExecutor;
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
  @StorageThreadPoolExecutor
  ThreadPoolExecutor provideThreadPoolExecutor(@StorageNumThreads int numThreads) {
    if (threadPoolExecutor == null) {
      threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(numThreads);
    }
    return threadPoolExecutor;
  }

  @Provides
  @Singleton
  Storage provideStorage(@StorageThreadPoolExecutor ThreadPoolExecutor threadPoolExecutor)
      throws IOException {
    if (storage == null) {
      storage = new StorageImpl(threadPoolExecutor);
    }
    return storage;
  }

}
