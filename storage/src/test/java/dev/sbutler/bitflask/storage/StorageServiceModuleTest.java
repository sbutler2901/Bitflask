package dev.sbutler.bitflask.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import dev.sbutler.bitflask.storage.configuration.StorageConfiguration;
import dev.sbutler.bitflask.storage.configuration.concurrency.StorageExecutorService;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import java.util.ResourceBundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StorageServiceModuleTest {

  private final StorageServiceModule storageServiceModule = StorageServiceModule.getInstance();

  @BeforeEach
  void beforeEach() {
    StorageServiceModule.setStorageConfiguration(new StorageConfiguration());
  }

  @Test
  void configure() {
    Injector injector = Guice.createInjector(StorageServiceModule.getInstance());
    try {
      injector.getBinding(
          Key.get(ListeningExecutorService.class).withAnnotation(StorageExecutorService.class));
      injector.getBinding(StorageService.class);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  void provideStorageDispatcherCapacity() {
    assertEquals(500, storageServiceModule.provideStorageDispatcherCapacity());
  }

  @Test
  void provideStorageDispatcherCapacity_withConfiguration() {
    // Arrange
    ResourceBundle resourceBundle = mock(ResourceBundle.class);
    doReturn(true).when(resourceBundle).containsKey("storageDispatcherCapacity");
    doReturn("100").when(resourceBundle).getString("storageDispatcherCapacity");
    StorageConfiguration storageConfiguration = new StorageConfiguration(resourceBundle);
    // Act
    StorageServiceModule.setStorageConfiguration(storageConfiguration);
    // Assert
    assertEquals(100, storageServiceModule.provideStorageDispatcherCapacity());
  }

  @Test
  void provideStorageCommandDispatcher() {
    StorageCommandDispatcher storageCommandDispatcher = storageServiceModule.provideStorageCommandDispatcher(
        1);
    assertEquals(storageCommandDispatcher, storageServiceModule.provideStorageCommandDispatcher(2));
  }
}
