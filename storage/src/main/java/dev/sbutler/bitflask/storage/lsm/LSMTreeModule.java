package dev.sbutler.bitflask.storage.lsm;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.util.concurrent.Executors;

/**
 * Guice module for {@link LSMTree} dependencies.
 */
public class LSMTreeModule extends AbstractModule {

  @Provides
  @Singleton
  @LSMTreeListeningScheduledExecutorService
  ListeningScheduledExecutorService provideListeningExecutorService() {
    return MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor());
  }
}
