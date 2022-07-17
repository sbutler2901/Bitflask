package dev.sbutler.bitflask.server.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.server.network_service.NetworkService;
import dev.sbutler.bitflask.storage.StorageService;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.Test;

public class ServerModuleTest {

  private final ServerModule serverModule = ServerModule.getInstance();

  @Test
  void configure() {
    Injector injector = Guice.createInjector(ServerModule.getInstance());
    injector.getBinding(StorageService.class);
    injector.getBinding(NetworkService.class);
    injector.getBinding(ExecutorService.class);
  }

  @Test
  void provideServerPort() {
    assertEquals(9090, serverModule.provideServerPort());
  }

}
