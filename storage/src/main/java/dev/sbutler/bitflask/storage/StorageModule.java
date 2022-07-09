package dev.sbutler.bitflask.storage;

import com.google.inject.AbstractModule;
import dev.sbutler.bitflask.storage.configuration.ConfigurationModule;
import dev.sbutler.bitflask.storage.segment.SegmentModule;

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

}
