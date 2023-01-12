package dev.sbutler.bitflask.server.configuration.concurrency;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import javax.inject.Singleton;

@Singleton
final class VirtualThreadFactory implements ThreadFactory {

  static final String NETWORK_SERVICE_THREAD_NAME = "virtual-pool-";

  private static final AtomicInteger threadNum = new AtomicInteger();

  @Override
  public Thread newThread(@Nonnull Runnable r) {
    String threadName = NETWORK_SERVICE_THREAD_NAME + threadNum.getAndIncrement();
    return Thread.ofVirtual().name(threadName).unstarted(r);
  }
}
