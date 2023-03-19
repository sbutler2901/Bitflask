package dev.sbutler.bitflask.storage;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.sbutler.bitflask.common.io.FilesHelper;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import dev.sbutler.bitflask.storage.lsm.LSMTreeModule;
import java.util.concurrent.ThreadFactory;
import javax.inject.Singleton;

/**
 * The root Guice module for executing the StorageService
 */
public class StorageServiceModule extends AbstractModule {

  private final StorageConfigurations storageConfigurations;

  public StorageServiceModule(StorageConfigurations storageConfigurations) {
    this.storageConfigurations = storageConfigurations;
  }

  @Override
  protected void configure() {
    install(new LSMTreeModule());
  }

  @Provides
  StorageConfigurations provideStorageConfiguration() {
    return storageConfigurations;
  }

  @Provides
  @Singleton
  StorageCommandDispatcher provideStorageCommandDispatcher(
      StorageConfigurations storageConfigurations) {
    return new StorageCommandDispatcher(
        storageConfigurations.getDispatcherCapacity());
  }

  @Provides
  FilesHelper provideFilesHelper(ThreadFactory threadFactory) {
    return new FilesHelper(threadFactory);
  }
}
