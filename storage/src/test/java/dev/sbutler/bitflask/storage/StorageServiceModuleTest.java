package dev.sbutler.bitflask.storage;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import java.util.concurrent.ThreadFactory;
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
    ThreadFactory threadFactory = mock(ThreadFactory.class);
    // Act
    storageServiceModule.provideFilesHelper(threadFactory);
  }
}
