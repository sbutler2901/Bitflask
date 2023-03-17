package dev.sbutler.bitflask.common.concurrency;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;

/**
 * A {@link java.util.concurrent.ThreadFactory} implementation that creates virtual threads.
 */
public final class VirtualThreadFactory implements ThreadFactory {

  public static final String DEFAULT_THREAD_NAME = "virtual-pool-";

  private final String threadNamePrefix;
  private final AtomicInteger threadNum = new AtomicInteger();

  /**
   * Creates a new instance that uses {@link VirtualThreadFactory#DEFAULT_THREAD_NAME} for new
   * threads.
   */
  public VirtualThreadFactory() {
    this.threadNamePrefix = DEFAULT_THREAD_NAME;
  }

  /**
   * Creates a new instance that uses the provided threadNamePrefix for new threads.
   */
  public VirtualThreadFactory(String threadNamePrefix) {
    this.threadNamePrefix = threadNamePrefix;
  }

  @Override
  public Thread newThread(@Nonnull Runnable r) {
    String threadName = threadNamePrefix + threadNum.getAndIncrement();
    return Thread.ofVirtual().name(threadName).unstarted(r);
  }
}
