package dev.sbutler.bitflask.storage.configuration.concurrency;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import javax.inject.Singleton;

@Singleton
class StorageThreadFactory implements ThreadFactory {

  private static final String STORAGE_SERVICE_THREAD_NAME = "storage-pool-";

  private static final AtomicInteger threadNum = new AtomicInteger();

  @Override
  public Thread newThread(@Nonnull Runnable r) {
    String threadName = STORAGE_SERVICE_THREAD_NAME + threadNum.getAndIncrement();
    return Thread.ofVirtual().name(threadName).unstarted(r);
  }
}
