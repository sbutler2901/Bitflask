package dev.sbutler.bitflask.storage;

import com.google.inject.AbstractModule;
import dev.sbutler.bitflask.storage.configuration.ConfigurationModule;
import dev.sbutler.bitflask.storage.segment.SegmentModule;

public class StorageServiceModule extends AbstractModule {

  private static final StorageServiceModule instance = new StorageServiceModule();

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

}
