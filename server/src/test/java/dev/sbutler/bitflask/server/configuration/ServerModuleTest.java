package dev.sbutler.bitflask.server.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.server.network_service.NetworkService;
import dev.sbutler.bitflask.storage.StorageService;
import java.util.concurrent.ThreadFactory;
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
    assertTrue(exception.getMessage().contains("ServerConfigurations"));
  }

  @Test
  void configure() {
    // Arrange
    ServerModule.setServerConfiguration(new ServerConfigurations());
    Injector injector = Guice.createInjector(ServerModule.getInstance());
    // Act / Assert
    /// Concurrency module
    injector.getBinding(ListeningExecutorService.class);
    injector.getBinding(ThreadFactory.class);
    /// Storage module
    injector.getBinding(StorageService.class);
    injector.getBinding(NetworkService.class);
  }

  @Test
  void provideServerConfiguration() {
    // Arrange
    ServerConfigurations expectedServerConfigurations = new ServerConfigurations();
    ServerModule.setServerConfiguration(expectedServerConfigurations);
    ServerModule serverModule = ServerModule.getInstance();
    // Act
    ServerConfigurations serverConfigurations = serverModule.provideServerConfiguration();
    // Assert
    assertEquals(expectedServerConfigurations, serverConfigurations);
  }
}
