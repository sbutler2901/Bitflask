package dev.sbutler.bitflask.storage;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.sbutler.bitflask.common.io.FilesHelper;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import java.util.concurrent.ThreadFactory;
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
    return instance;
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
