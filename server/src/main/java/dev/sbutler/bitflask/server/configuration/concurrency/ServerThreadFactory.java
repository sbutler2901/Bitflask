package dev.sbutler.bitflask.server.configuration.concurrency;

import java.util.concurrent.ThreadFactory;
import javax.annotation.Nonnull;

class ServerThreadFactory implements ThreadFactory {

  private static final String NETWORK_SERVICE_THREAD_NAME = "server-pool-";

  private static int threadNum = 0;

  @Override
  public Thread newThread(@Nonnull Runnable r) {
    String threadName = NETWORK_SERVICE_THREAD_NAME + threadNum++;
    return new Thread(r, threadName);
  }
}
