package dev.sbutler.bitflask.storage;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.sbutler.bitflask.storage.configuration.ConfigurationModule;
import dev.sbutler.bitflask.storage.segment.SegmentModule;
import javax.inject.Singleton;

public class StorageModule extends AbstractModule {

  private static final StorageModule instance = new StorageModule();

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
  StorageService provideStorageService(StorageImpl storageService) {
    return storageService;
  }

}
