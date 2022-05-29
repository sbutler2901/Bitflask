package dev.sbutler.bitflask.storage;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.sbutler.bitflask.storage.configuration.ConfigurationModule;
import dev.sbutler.bitflask.storage.configuration.concurrency.StorageExecutorService;
import dev.sbutler.bitflask.storage.segment.SegmentManager;
import dev.sbutler.bitflask.storage.segment.SegmentModule;
import java.util.concurrent.ExecutorService;
import javax.inject.Singleton;

public class StorageModule extends AbstractModule {

  private static final StorageModule instance = new StorageModule();

  private Storage storage;

  private StorageModule() {
  }

  public static StorageModule getInstance() {
    return instance;
  }

  @Override
  protected void configure() {
    super.configure();
    install(ConfigurationModule.getInstance());
    install(new SegmentModule());
  }

  @Provides
  @Singleton
  Storage provideStorage(@StorageExecutorService ExecutorService executorService,
      SegmentManager segmentManager) {
    if (storage == null) {
      storage = new StorageImpl(executorService, segmentManager);
    }
    return storage;
  }

}
