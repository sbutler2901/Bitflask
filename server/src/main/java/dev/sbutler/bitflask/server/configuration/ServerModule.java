package dev.sbutler.bitflask.server.configuration;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.sbutler.bitflask.server.configuration.concurrency.ConcurrencyModule;
import dev.sbutler.bitflask.server.network_service.NetworkServiceModule;
import dev.sbutler.bitflask.storage.StorageServiceModule;

public class ServerModule extends AbstractModule {

  private static final ServerModule instance = new ServerModule();

  private ServerModule() {
  }

  public static ServerModule getInstance() {
    return instance;
  }

  @Override
  protected void configure() {
    super.configure();
    install(ConcurrencyModule.getInstance());
    install(StorageServiceModule.getInstance());
    install(new NetworkServiceModule());
  }

  @Provides
  @ServerPort
  int provideServerPort() {
    return 9090;
  }

  @Provides
  @ServerNumThreads
  int provideServerNumThreads() {
    return 4;
  }

  @Provides
  @ServerCommandDispatcherCapacity
  int provideServerCommandDispatcherCapacity() {
    return 500;
  }
}
