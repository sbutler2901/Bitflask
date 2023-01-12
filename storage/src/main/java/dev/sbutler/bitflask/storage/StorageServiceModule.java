package dev.sbutler.bitflask.storage;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.sbutler.bitflask.common.io.FilesHelper;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import java.util.concurrent.ThreadFactory;
import javax.inject.Singleton;

public class StorageServiceModule extends AbstractModule {

  private final StorageConfigurations storageConfigurations;

  private StorageCommandDispatcher storageCommandDispatcher = null;

  public StorageServiceModule(StorageConfigurations storageConfigurations) {
    this.storageConfigurations = storageConfigurations;
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
  FilesHelper provideFilesHelper(ThreadFactory threadFactory) {
    return new FilesHelper(threadFactory);
  }
}
