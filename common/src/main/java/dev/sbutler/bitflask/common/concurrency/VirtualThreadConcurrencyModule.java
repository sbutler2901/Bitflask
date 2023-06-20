package dev.sbutler.bitflask.common.concurrency;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import jakarta.inject.Singleton;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public final class VirtualThreadConcurrencyModule extends AbstractModule {

  @Provides
  @Singleton
  ListeningExecutorService provideExecutorService(VirtualThreadFactory virtualThreadFactory) {
    return MoreExecutors.listeningDecorator(
        Executors.newThreadPerTaskExecutor(virtualThreadFactory));
  }

  @Provides
  @Singleton
  ThreadFactory provideThreadFactory(VirtualThreadFactory virtualThreadFactory) {
    return virtualThreadFactory;
  }
}
