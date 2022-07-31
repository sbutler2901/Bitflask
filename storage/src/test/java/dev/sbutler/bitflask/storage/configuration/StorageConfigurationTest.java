package dev.sbutler.bitflask.storage.configuration;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import org.junit.jupiter.api.Test;

public class StorageConfigurationTest {

  @Test
  void defaultConfiguration() {
    // Arrange
    Path defaultSegmentDirPath = Paths.get(System.getProperty("user.home") + "/.bitflask/store/");
    // Act
    StorageConfiguration storageConfiguration = new StorageConfiguration();
    // Assert
    assertEquals(500, storageConfiguration.getStorageDispatcherCapacity());
    assertEquals(defaultSegmentDirPath, storageConfiguration.getSegmentDirPath());
  }

  @Test
  void propertyFile() {
    // Arrange
    ResourceBundle resourceBundle = mock(ResourceBundle.class);
    doReturn(true).when(resourceBundle).containsKey("storage.dispatcherCapacity");
    doReturn("100").when(resourceBundle).getString("storage.dispatcherCapacity");
    doReturn(true).when(resourceBundle).containsKey("storage.storeDirectory");
    Path expectedSegmentDirPath = Paths.get(System.getProperty("user.home") + "/.bitflask/");
    doReturn(expectedSegmentDirPath.toString()).when(resourceBundle)
        .getString("storage.storeDirectory");
    // Act
    StorageConfiguration storageConfiguration = new StorageConfiguration(resourceBundle);
    // Assert
    assertEquals(100, storageConfiguration.getStorageDispatcherCapacity());
    assertEquals(expectedSegmentDirPath, storageConfiguration.getSegmentDirPath());
  }

  @Test
  void commandLineFlags() {
    // Arrange
    StorageConfiguration storageConfiguration = new StorageConfiguration();
    Path expectedSegmentDirPath = Paths.get("/tmp/.bitflask");
    String[] argv = new String[]{"--storageDispatcherCapacity", "100", "--storageStoreDirectory",
        expectedSegmentDirPath.toString()};
    // Act
    JCommander.newBuilder()
        .addObject(storageConfiguration)
        .build()
        .parse(argv);
    // Assert
    assertEquals(100, storageConfiguration.getStorageDispatcherCapacity());
    assertEquals(expectedSegmentDirPath, storageConfiguration.getSegmentDirPath());
  }

  @Test
  void commandLineFlags_withPropertyFile() {
    // Arrange
    ResourceBundle resourceBundle = mock(ResourceBundle.class);
    doReturn(true).when(resourceBundle).containsKey("storage.dispatcherCapacity");
    doReturn("200").when(resourceBundle).getString("storage.dispatcherCapacity");
    doReturn(true).when(resourceBundle).containsKey("storage.storeDirectory");
    Path configSegmentDirPath = Paths.get(System.getProperty("user.home") + "/.bitflask/");
    doReturn(configSegmentDirPath.toString()).when(resourceBundle)
        .getString("storage.storeDirectory");
    StorageConfiguration storageConfiguration = new StorageConfiguration(resourceBundle);
    Path expectedSegmentDirPath = Paths.get("/tmp/.bitflask");
    String[] argv = new String[]{"--storageDispatcherCapacity", "100", "--storageStoreDirectory",
        expectedSegmentDirPath.toString()};
    // Act
    JCommander.newBuilder()
        .addObject(storageConfiguration)
        .build()
        .parse(argv);
    // Assert
    assertEquals(100, storageConfiguration.getStorageDispatcherCapacity());
    assertEquals(expectedSegmentDirPath, storageConfiguration.getSegmentDirPath());
  }

  @Test
  void propertyFile_storeDirectory_absolutePathOnly() {
    // Arrange
    Path defaultSegmentDirPath = (new StorageConfiguration()).getSegmentDirPath();
    ResourceBundle resourceBundle = mock(ResourceBundle.class);
    doReturn(true).when(resourceBundle).containsKey("storage.storeDirectory");
    Path configSegmentDirPath = Paths.get("~/.bitflask/");
    doReturn(configSegmentDirPath.toString()).when(resourceBundle)
        .getString("storage.storeDirectory");
    // Act
    StorageConfiguration storageConfiguration = new StorageConfiguration(resourceBundle);
    // Assert
    assertEquals(defaultSegmentDirPath, storageConfiguration.getSegmentDirPath());
  }

  @Test
  void commandLineFlags_storeDirectory_absolutePathOnly() {
    // Arrange
    StorageConfiguration storageConfiguration = new StorageConfiguration();
    String[] argv = new String[]{"--storageStoreDirectory", "~/.bitflask/"};
    // Act / Assert
    assertThrows(ParameterException.class, () ->
        JCommander.newBuilder()
            .addObject(storageConfiguration)
            .build()
            .parse(argv)
    );
  }
}
