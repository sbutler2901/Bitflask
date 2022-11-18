package dev.sbutler.bitflask.storage;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.sbutler.bitflask.common.io.FilesHelper;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.configuration.concurrency.ConcurrencyModule;
import dev.sbutler.bitflask.storage.configuration.concurrency.StorageThreadFactory;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import javax.inject.Singleton;

public class StorageServiceModule extends AbstractModule {

  private static final StorageServiceModule instance = new StorageServiceModule();
  private static StorageConfigurations storageConfigurations = new StorageConfigurations();

  private StorageCommandDispatcher storageCommandDispatcher = null;

  private StorageServiceModule() {
  }

  public static void setStorageConfiguration(StorageConfigurations storageConfigurations) {
    StorageServiceModule.storageConfigurations = storageConfigurations;
  }

  public static StorageServiceModule getInstance() {
    if (storageConfigurations == null) {
      throw new IllegalStateException(
          "The StorageServiceModule must have the StorageConfigurations set before it can be used");
    }
    return instance;
  }

  @Override
  protected void configure() {
    super.configure();
    install(ConcurrencyModule.getInstance());
  }

  @Provides
  StorageConfigurations provideStorageConfiguration() {
    return storageConfigurations;
  }

  @Provides
  @Singleton
  StorageCommandDispatcher provideStorageCommandDispatcher(
      StorageConfigurations storageConfigurations) {
    if (storageCommandDispatcher == null) {
      storageCommandDispatcher = new StorageCommandDispatcher(
          storageConfigurations.getStorageDispatcherCapacity());
    }
    return storageCommandDispatcher;
  }

  @Provides
  FilesHelper provideFilesHelper(StorageThreadFactory storageThreadFactory) {
    return new FilesHelper(storageThreadFactory);
  }
}
