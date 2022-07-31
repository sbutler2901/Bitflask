package dev.sbutler.bitflask.server.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.server.network_service.NetworkService;
import dev.sbutler.bitflask.storage.StorageService;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.Test;

public class ServerModuleTest {

  @Test
  void configure() {
    ServerConfiguration serverConfiguration = mock(ServerConfiguration.class);
    ServerModule serverModule = new ServerModule(serverConfiguration);
    Injector injector = Guice.createInjector(serverModule);
    injector.getBinding(StorageService.class);
    injector.getBinding(NetworkService.class);
    injector.getBinding(ExecutorService.class);
  }

  @Test
  void provideServerPort() {
    ServerConfiguration serverConfiguration = mock(ServerConfiguration.class);
    doReturn(9090).when(serverConfiguration).getPort();
    ServerModule serverModule = new ServerModule(serverConfiguration);
    assertEquals(9090, serverModule.provideServerPort());
  }

}
