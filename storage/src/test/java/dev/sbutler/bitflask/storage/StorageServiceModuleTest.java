package dev.sbutler.bitflask.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import dev.sbutler.bitflask.storage.configuration.StorageConfiguration;
import dev.sbutler.bitflask.storage.configuration.concurrency.StorageExecutorService;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import org.junit.jupiter.api.Test;

public class StorageServiceModuleTest {

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Test
  void getInstanceFailsWithoutStorageConfiguration() {
    // Arrange
    StorageServiceModule.setStorageConfiguration(null);
    // Act
    IllegalStateException exception =
        assertThrows(IllegalStateException.class, StorageServiceModule::getInstance);
    // Assert
    assertTrue(exception.getMessage().contains("StorageConfiguration"));
  }

  @Test
  void configure() {
    // Arrange
    StorageServiceModule.setStorageConfiguration(new StorageConfiguration());
    Injector injector = Guice.createInjector(StorageServiceModule.getInstance());
    // Act / Assert
    injector.getBinding(
        Key.get(ListeningExecutorService.class).withAnnotation(StorageExecutorService.class));
    injector.getBinding(StorageService.class);
  }

  @Test
  void provideStorageConfiguration() {
    // Arrange
    StorageConfiguration setStorageConfiguration = new StorageConfiguration();
    StorageServiceModule.setStorageConfiguration(setStorageConfiguration);
    StorageServiceModule storageServiceModule = StorageServiceModule.getInstance();
    // Act
    StorageConfiguration storageConfiguration = storageServiceModule.provideStorageConfiguration();
    // Assert
    assertEquals(setStorageConfiguration, storageConfiguration);
  }

  @Test
  void provideStorageCommandDispatcher() {
    // Arrange
    StorageConfiguration storageConfiguration = mock(StorageConfiguration.class);
    doReturn(1).when(storageConfiguration).getStorageDispatcherCapacity();
    StorageServiceModule.setStorageConfiguration(storageConfiguration);
    StorageServiceModule storageServiceModule = StorageServiceModule.getInstance();
    // Act
    StorageCommandDispatcher storageCommandDispatcher =
        storageServiceModule.provideStorageCommandDispatcher(storageConfiguration);
    // Assert
    assertEquals(storageCommandDispatcher,
        storageServiceModule.provideStorageCommandDispatcher(storageConfiguration));
  }
}
