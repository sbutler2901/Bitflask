package dev.sbutler.bitflask.storage;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.configuration.concurrency.StorageExecutorService;
import dev.sbutler.bitflask.storage.configuration.concurrency.StorageThreadFactory;
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
    assertThat(exception).hasMessageThat().ignoringCase().contains("StorageConfigurations");
  }

  @Test
  void configure() {
    // Arrange
    StorageServiceModule.setStorageConfiguration(new StorageConfigurations());
    Injector injector = Guice.createInjector(StorageServiceModule.getInstance());
    // Act / Assert
    injector.getBinding(
        Key.get(ListeningExecutorService.class).withAnnotation(StorageExecutorService.class));
    injector.getBinding(StorageService.class);
  }

  @Test
  void provideStorageConfiguration() {
    // Arrange
    StorageConfigurations setStorageConfigurations = new StorageConfigurations();
    StorageServiceModule.setStorageConfiguration(setStorageConfigurations);
    StorageServiceModule storageServiceModule = StorageServiceModule.getInstance();
    // Act
    StorageConfigurations storageConfigurations = storageServiceModule.provideStorageConfiguration();
    // Assert
    assertThat(storageConfigurations).isEqualTo(setStorageConfigurations);
  }

  @Test
  void provideStorageCommandDispatcher() {
    // Arrange
    StorageConfigurations storageConfigurations = mock(StorageConfigurations.class);
    when(storageConfigurations.getStorageDispatcherCapacity()).thenReturn(1);
    StorageServiceModule.setStorageConfiguration(storageConfigurations);
    StorageServiceModule storageServiceModule = StorageServiceModule.getInstance();
    // Act
    StorageCommandDispatcher storageCommandDispatcher =
        storageServiceModule.provideStorageCommandDispatcher(storageConfigurations);
    // Assert
    assertThat(storageCommandDispatcher)
        .isEqualTo(storageServiceModule.provideStorageCommandDispatcher(storageConfigurations));
  }

  @Test
  void provideFilesHelper() {
    // Arrange
    StorageConfigurations setStorageConfigurations = new StorageConfigurations();
    StorageServiceModule.setStorageConfiguration(setStorageConfigurations);
    StorageServiceModule storageServiceModule = StorageServiceModule.getInstance();
    StorageThreadFactory threadFactory = mock(StorageThreadFactory.class);
    // Act
    storageServiceModule.provideFilesHelper(threadFactory);
  }
}
