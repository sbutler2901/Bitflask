package dev.sbutler.bitflask.server.configuration;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.sbutler.bitflask.server.configuration.logging.LoggingModule;
import dev.sbutler.bitflask.server.network_service.NetworkServiceModule;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Singleton;

public class ServerModule extends AbstractModule {

  private static final ServerModule instance = new ServerModule();

  private ExecutorService executorService;

  private ServerModule() {
  }

  public static ServerModule getInstance() {
    return instance;
  }

  @Override
  protected void configure() {
    super.configure();
    install(new NetworkServiceModule());
    install(new LoggingModule());
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
  @Singleton
  ExecutorService provideExecutorService(@ServerNumThreads int numThreads) {
    if (executorService == null) {
      executorService = Executors.newFixedThreadPool(numThreads);
    }
    return executorService;
  }

}
