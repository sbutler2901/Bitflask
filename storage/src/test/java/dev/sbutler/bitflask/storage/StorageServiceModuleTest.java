package dev.sbutler.bitflask.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import dev.sbutler.bitflask.storage.configuration.StorageConfiguration;
import dev.sbutler.bitflask.storage.configuration.concurrency.StorageExecutorService;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
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
    injector.getBinding(
        Key.get(ListeningExecutorService.class).withAnnotation(StorageExecutorService.class));
    injector.getBinding(StorageService.class);
  }

  @Test
  void provideStorageDispatcherCapacity() {
    assertEquals(500, storageServiceModule.provideStorageDispatcherCapacity());
  }

  @Test
  void provideStorageDispatcherCapacity_withConfiguration() {
    // Arrange
    StorageConfiguration storageConfiguration = mock(StorageConfiguration.class);
    doReturn(100).when(storageConfiguration).getStorageDispatcherCapacity();
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
