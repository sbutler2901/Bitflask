package dev.sbutler.bitflask.storage.configuration;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.beust.jcommander.JCommander;
import java.util.ResourceBundle;
import org.junit.jupiter.api.Test;

public class StorageConfigurationTest {

  @Test
  void defaultConfiguration() {
    // Act
    StorageConfiguration storageConfiguration = new StorageConfiguration();
    // Assert
    assertEquals(500, storageConfiguration.getStorageDispatcherCapacity());
  }

  @Test
  void propertyFile() {
    // Arrange
    ResourceBundle resourceBundle = mock(ResourceBundle.class);
    doReturn(true).when(resourceBundle).containsKey("storageDispatcherCapacity");
    doReturn("100").when(resourceBundle).getString("storageDispatcherCapacity");
    // Act
    StorageConfiguration storageConfiguration = new StorageConfiguration(resourceBundle);
    // Assert
    assertEquals(100, storageConfiguration.getStorageDispatcherCapacity());
  }

  @Test
  void commandLineFlags() {
    // Arrange
    StorageConfiguration storageConfiguration = new StorageConfiguration();
    String[] argv = new String[]{"--storageDispatcherCapacity", "100"};
    // Act
    JCommander.newBuilder()
        .addObject(storageConfiguration)
        .build()
        .parse(argv);
    // Assert
    assertEquals(100, storageConfiguration.getStorageDispatcherCapacity());
  }

  @Test
  void commandLineFlags_withPropertyFile() {
    // Arrange
    ResourceBundle resourceBundle = mock(ResourceBundle.class);
    doReturn(true).when(resourceBundle).containsKey("storageDispatcherCapacity");
    doReturn("200").when(resourceBundle).getString("storageDispatcherCapacity");
    StorageConfiguration storageConfiguration = new StorageConfiguration(resourceBundle);
    String[] argv = new String[]{"--storageDispatcherCapacity", "100"};
    // Act
    JCommander.newBuilder()
        .addObject(storageConfiguration)
        .build()
        .parse(argv);
    // Assert
    assertEquals(100, storageConfiguration.getStorageDispatcherCapacity());
  }
}
