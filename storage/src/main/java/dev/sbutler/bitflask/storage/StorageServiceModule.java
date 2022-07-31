package dev.sbutler.bitflask.storage;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.sbutler.bitflask.storage.configuration.StorageConfiguration;
import dev.sbutler.bitflask.storage.configuration.StorageDispatcherCapacity;
import dev.sbutler.bitflask.storage.configuration.StorageStoreDirectoryPath;
import dev.sbutler.bitflask.storage.configuration.concurrency.ConcurrencyModule;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import dev.sbutler.bitflask.storage.segment.SegmentModule;
import java.nio.file.Path;
import javax.inject.Singleton;

public class StorageServiceModule extends AbstractModule {

  private static final StorageServiceModule instance = new StorageServiceModule();
  private static StorageConfiguration storageConfiguration = new StorageConfiguration();

  private StorageCommandDispatcher storageCommandDispatcher = null;

  private StorageServiceModule() {
  }

  public static StorageServiceModule getInstance() {
    return instance;
  }

  public static void setStorageConfiguration(StorageConfiguration storageConfiguration) {
    StorageServiceModule.storageConfiguration = storageConfiguration;
  }

  @Override
  protected void configure() {
    super.configure();
    install(ConcurrencyModule.getInstance());
    install(new SegmentModule());
  }

  @Provides
  @StorageStoreDirectoryPath
  Path provideStorageStoreDirectoryPath() {
    return storageConfiguration.getStorageStoreDirectoryPath();
  }

  @Provides
  @StorageDispatcherCapacity
  int provideStorageDispatcherCapacity() {
    return storageConfiguration.getStorageDispatcherCapacity();
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
