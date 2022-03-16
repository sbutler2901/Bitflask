package dev.sbutler.bitflask.server.configuration;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.matcher.Matchers;
import dev.sbutler.bitflask.server.configuration.logging.Slf4jTypeListener;
import dev.sbutler.bitflask.server.network_service.NetworkServiceModule;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    bindListener(Matchers.any(), new Slf4jTypeListener());
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
