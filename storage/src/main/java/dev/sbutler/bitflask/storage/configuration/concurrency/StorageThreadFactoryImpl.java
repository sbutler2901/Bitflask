package dev.sbutler.bitflask.storage.configuration.concurrency;

import java.util.concurrent.ThreadFactory;
import javax.annotation.Nonnull;

public final class StorageThreadFactoryImpl implements ThreadFactory {

  private static final String STORAGE_SERVICE_THREAD_NAME = "storage-pool-";

  private static int threadNum = 0;

  @Override
  public Thread newThread(@Nonnull Runnable r) {
    String threadName = STORAGE_SERVICE_THREAD_NAME + threadNum++;
    return new Thread(r, threadName);
  }

}
