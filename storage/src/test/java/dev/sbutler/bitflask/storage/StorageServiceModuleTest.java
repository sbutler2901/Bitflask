package dev.sbutler.bitflask.storage;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import java.util.concurrent.ThreadFactory;
import org.junit.jupiter.api.Test;

public class StorageServiceModuleTest {

  @Test
  void provideStorageConfiguration() {
    // Arrange
    StorageConfigurations storageConfigurations = new StorageConfigurations();
    StorageServiceModule storageServiceModule = new StorageServiceModule(storageConfigurations);
    // Act
    StorageConfigurations providedStorageConfigurations = storageServiceModule.provideStorageConfiguration();
    // Assert
    assertThat(providedStorageConfigurations).isEqualTo(storageConfigurations);
  }

  @Test
  void provideStorageCommandDispatcher() {
    // Arrange
    StorageConfigurations storageConfigurations = mock(StorageConfigurations.class);
    StorageServiceModule storageServiceModule = new StorageServiceModule(storageConfigurations);
    when(storageConfigurations.getStorageDispatcherCapacity()).thenReturn(1);
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
    StorageConfigurations storageConfigurations = new StorageConfigurations();
    StorageServiceModule storageServiceModule = new StorageServiceModule(storageConfigurations);
    ThreadFactory threadFactory = mock(ThreadFactory.class);
    // Act
    storageServiceModule.provideFilesHelper(threadFactory);
  }
}
