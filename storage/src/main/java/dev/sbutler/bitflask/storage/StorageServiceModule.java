package dev.sbutler.bitflask.storage;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.sbutler.bitflask.storage.configuration.ConfigurationModule;
import dev.sbutler.bitflask.storage.configuration.StorageDispatcherCapacity;
import dev.sbutler.bitflask.storage.segment.SegmentModule;
import javax.inject.Singleton;

public class StorageServiceModule extends AbstractModule {

  private static final StorageServiceModule instance = new StorageServiceModule();

  private StorageCommandDispatcher storageCommandDispatcher = null;

  private StorageServiceModule() {
  }

  public static StorageServiceModule getInstance() {
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
  StorageCommandDispatcher provideStorageCommandDispatcher(
      @StorageDispatcherCapacity int capacity) {
    if (storageCommandDispatcher == null) {
      storageCommandDispatcher = new StorageCommandDispatcher(capacity);
    }
    return storageCommandDispatcher;
  }
}
