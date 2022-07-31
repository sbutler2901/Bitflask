package dev.sbutler.bitflask.server.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.server.network_service.NetworkService;
import dev.sbutler.bitflask.storage.StorageService;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServerModuleTest {

  private final ServerModule serverModule = ServerModule.getInstance();

  @BeforeEach
  void beforeEach() {
    ServerModule.setServerConfiguration(new ServerConfiguration());
  }

  @Test
  void configure() {
    Injector injector = Guice.createInjector(serverModule);
    injector.getBinding(StorageService.class);
    injector.getBinding(NetworkService.class);
    injector.getBinding(ExecutorService.class);
  }

  @Test
  void provideServerPort() {
    assertEquals(9090, serverModule.provideServerPort());
  }

  @Test
  void provideServerPort_withConfiguration() {
    // Arrange
    ServerConfiguration serverConfiguration = mock(ServerConfiguration.class);
    doReturn(9091).when(serverConfiguration).getPort();
    // Act
    ServerModule.setServerConfiguration(serverConfiguration);
    // Assert
    assertEquals(9091, serverModule.provideServerPort());
  }

}
