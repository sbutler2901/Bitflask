package dev.sbutler.bitflask.storage;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.sbutler.bitflask.common.utils.FilesHelper;
import dev.sbutler.bitflask.storage.configuration.StorageConfiguration;
import dev.sbutler.bitflask.storage.configuration.concurrency.ConcurrencyModule;
import dev.sbutler.bitflask.storage.configuration.concurrency.StorageThreadFactory;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import javax.inject.Singleton;

public class StorageServiceModule extends AbstractModule {

  private static final StorageServiceModule instance = new StorageServiceModule();
  private static StorageConfiguration storageConfiguration = new StorageConfiguration();

  private StorageCommandDispatcher storageCommandDispatcher = null;

  private StorageServiceModule() {
  }

  public static void setStorageConfiguration(StorageConfiguration storageConfiguration) {
    StorageServiceModule.storageConfiguration = storageConfiguration;
  }

  public static StorageServiceModule getInstance() {
    if (storageConfiguration == null) {
      throw new IllegalStateException(
          "The StorageServiceModule must have the StorageConfiguration set before it can be used");
    }
    return instance;
  }

  @Override
  protected void configure() {
    super.configure();
    install(ConcurrencyModule.getInstance());
  }

  @Provides
  StorageConfiguration provideStorageConfiguration() {
    return storageConfiguration;
  }

  @Provides
  @Singleton
  StorageCommandDispatcher provideStorageCommandDispatcher(
      StorageConfiguration storageConfiguration) {
    if (storageCommandDispatcher == null) {
      storageCommandDispatcher = new StorageCommandDispatcher(
          storageConfiguration.getStorageDispatcherCapacity());
    }
    return storageCommandDispatcher;
  }

  @Provides
  FilesHelper provideFilesHelper(StorageThreadFactory storageThreadFactory) {
    return new FilesHelper(storageThreadFactory);
  }
}
