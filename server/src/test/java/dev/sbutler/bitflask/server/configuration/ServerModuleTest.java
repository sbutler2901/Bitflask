package dev.sbutler.bitflask.server.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.server.network_service.NetworkService;
import dev.sbutler.bitflask.storage.StorageService;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.Test;

public class ServerModuleTest {

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Test
  void getInstanceFailsWithoutServerConfiguration() {
    // Arrange
    ServerModule.setServerConfiguration(null);
    // Act
    IllegalStateException exception =
        assertThrows(IllegalStateException.class, ServerModule::getInstance);
    // Assert
    assertTrue(exception.getMessage().contains("ServerConfiguration"));
  }

  @Test
  void configure() {
    // Arrange
    ServerModule.setServerConfiguration(new ServerConfiguration());
    Injector injector = Guice.createInjector(ServerModule.getInstance());
    // Act / Assert
    injector.getBinding(StorageService.class);
    injector.getBinding(NetworkService.class);
    injector.getBinding(ExecutorService.class);
  }

  @Test
  void provideServerConfiguration() {
    // Arrange
    ServerConfiguration expectedServerConfiguration = new ServerConfiguration();
    ServerModule.setServerConfiguration(expectedServerConfiguration);
    ServerModule serverModule = ServerModule.getInstance();
    // Act
    ServerConfiguration serverConfiguration = serverModule.provideServerConfiguration();
    // Assert
    assertEquals(expectedServerConfiguration, serverConfiguration);
  }
}
