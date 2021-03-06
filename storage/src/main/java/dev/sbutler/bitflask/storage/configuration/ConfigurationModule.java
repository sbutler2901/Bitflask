package dev.sbutler.bitflask.storage.configuration;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.sbutler.bitflask.storage.configuration.concurrency.ConcurrencyModule;

public class ConfigurationModule extends AbstractModule {

  private static final ConfigurationModule instance = new ConfigurationModule();

  private ConfigurationModule() {
  }

  public static ConfigurationModule getInstance() {
    return instance;
  }

  @Override
  protected void configure() {
    super.configure();
    install(ConcurrencyModule.getInstance());
  }

  @Provides
  @StorageDispatcherCapacity
  int provideStorageDispatcherCapacity() {
    return 500;
  }

}
