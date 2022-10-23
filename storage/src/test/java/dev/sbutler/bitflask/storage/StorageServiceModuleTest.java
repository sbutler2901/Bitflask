package dev.sbutler.bitflask.storage;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import dev.sbutler.bitflask.storage.configuration.StorageConfiguration;
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
    assertThat(exception).hasMessageThat().ignoringCase().contains("StorageConfiguration");
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
    assertThat(storageConfiguration).isEqualTo(setStorageConfiguration);
  }

  @Test
  void provideStorageCommandDispatcher() {
    // Arrange
    StorageConfiguration storageConfiguration = mock(StorageConfiguration.class);
    when(storageConfiguration.getStorageDispatcherCapacity()).thenReturn(1);
    StorageServiceModule.setStorageConfiguration(storageConfiguration);
    StorageServiceModule storageServiceModule = StorageServiceModule.getInstance();
    // Act
    StorageCommandDispatcher storageCommandDispatcher =
        storageServiceModule.provideStorageCommandDispatcher(storageConfiguration);
    // Assert
    assertThat(storageCommandDispatcher)
        .isEqualTo(storageServiceModule.provideStorageCommandDispatcher(storageConfiguration));
  }

  @Test
  void provideFilesHelper() {
    // Arrange
    StorageConfiguration setStorageConfiguration = new StorageConfiguration();
    StorageServiceModule.setStorageConfiguration(setStorageConfiguration);
    StorageServiceModule storageServiceModule = StorageServiceModule.getInstance();
    StorageThreadFactory threadFactory = mock(StorageThreadFactory.class);
    // Act
    storageServiceModule.provideFilesHelper(threadFactory);
  }
}
