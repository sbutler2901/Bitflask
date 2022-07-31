package dev.sbutler.bitflask.storage.configuration;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.beust.jcommander.JCommander;
import dev.sbutler.bitflask.common.configuration.exceptions.IllegalConfigurationException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

public class StorageConfigurationTest {

  @Test
  void propertyFile() {
    // Arrange
    StorageConfigurationDefaultProvider defaultProvider = new StorageConfigurationDefaultProvider();
    StorageConfiguration storageConfiguration = new StorageConfiguration();
    String[] argv = new String[]{};
    // Act
    JCommander.newBuilder()
        .addObject(storageConfiguration)
        .defaultProvider(defaultProvider)
        .build()
        .parse(argv);
    // Assert
    assertEquals(
        Integer.parseInt(defaultProvider.getDefaultValueFor(
            StorageConfiguration.STORAGE_DISPATCHER_CAPACITY_FLAG)),
        storageConfiguration.getStorageDispatcherCapacity());
    assertEquals(
        Path.of(defaultProvider.getDefaultValueFor(
            StorageConfiguration.STORAGE_STORE_DIRECTORY_PATH_FLAG)),
        storageConfiguration.getSegmentDirPath());
  }

  @Test
  void propertyFile_illegalConfiguration_storageDispatcherCapacity() {
    // Arrange
    StorageConfigurationDefaultProvider defaultProvider = mock(
        StorageConfigurationDefaultProvider.class);
    doReturn("-1").when(defaultProvider)
        .getDefaultValueFor(StorageConfiguration.STORAGE_DISPATCHER_CAPACITY_FLAG);
    doReturn("/tmp/.bitflask").when(defaultProvider)
        .getDefaultValueFor(StorageConfiguration.STORAGE_STORE_DIRECTORY_PATH_FLAG);

    StorageConfiguration storageConfiguration = new StorageConfiguration();
    String[] argv = new String[]{};
    // Act
    IllegalConfigurationException exception =
        assertThrows(IllegalConfigurationException.class,
            () -> JCommander.newBuilder()
                .addObject(storageConfiguration)
                .defaultProvider(defaultProvider)
                .build()
                .parse(argv));
    // Assert
    assertTrue(
        exception.getMessage().contains(StorageConfiguration.STORAGE_DISPATCHER_CAPACITY_FLAG));
  }

  @Test
  void propertyFile_illegalConfiguration_storeDirectoryFlag() {
    // Arrange
    StorageConfigurationDefaultProvider defaultProvider = mock(
        StorageConfigurationDefaultProvider.class);
    doReturn("1").when(defaultProvider)
        .getDefaultValueFor(StorageConfiguration.STORAGE_DISPATCHER_CAPACITY_FLAG);
    doReturn("~/.bitflask").when(defaultProvider)
        .getDefaultValueFor(StorageConfiguration.STORAGE_STORE_DIRECTORY_PATH_FLAG);

    StorageConfiguration storageConfiguration = new StorageConfiguration();
    String[] argv = new String[]{};
    // Act
    IllegalConfigurationException exception =
        assertThrows(IllegalConfigurationException.class, () ->
            JCommander.newBuilder()
                .addObject(storageConfiguration)
                .defaultProvider(defaultProvider)
                .build()
                .parse(argv));
    // Assert
    assertTrue(
        exception.getMessage().contains(StorageConfiguration.STORAGE_STORE_DIRECTORY_PATH_FLAG));
  }

  @Test
  void commandLineFlags() {
    // Arrange
    StorageConfigurationDefaultProvider defaultProvider = new StorageConfigurationDefaultProvider();
    StorageConfiguration storageConfiguration = new StorageConfiguration();
    Path expectedSegmentDirPath = Paths.get("/random/absolute/path");
    String[] argv = new String[]{
        StorageConfiguration.STORAGE_DISPATCHER_CAPACITY_FLAG,
        "100",
        StorageConfiguration.STORAGE_STORE_DIRECTORY_PATH_FLAG,
        expectedSegmentDirPath.toString()};
    // Act
    JCommander.newBuilder()
        .addObject(storageConfiguration)
        .defaultProvider(defaultProvider)
        .build()
        .parse(argv);
    // Assert
    assertEquals(100, storageConfiguration.getStorageDispatcherCapacity());
    assertEquals(expectedSegmentDirPath, storageConfiguration.getSegmentDirPath());
  }

  @Test
  void commandLineFlags_illegalConfiguration_storageDispatcherCapacity() {
    // Arrange
    StorageConfigurationDefaultProvider defaultProvider = new StorageConfigurationDefaultProvider();
    StorageConfiguration storageConfiguration = new StorageConfiguration();
    String[] argv = new String[]{
        StorageConfiguration.STORAGE_DISPATCHER_CAPACITY_FLAG,
        "-1",
        StorageConfiguration.STORAGE_STORE_DIRECTORY_PATH_FLAG,
        "/random/absolute/path"};
    // Act
    IllegalConfigurationException exception =
        assertThrows(IllegalConfigurationException.class,
            () -> JCommander.newBuilder()
                .addObject(storageConfiguration)
                .defaultProvider(defaultProvider)
                .build()
                .parse(argv));
    // Assert
    assertTrue(
        exception.getMessage().contains(StorageConfiguration.STORAGE_DISPATCHER_CAPACITY_FLAG));
  }

  @Test
  void commandLineFlags_illegalConfiguration_storeDirectoryFlag() {
    // Arrange
    StorageConfigurationDefaultProvider defaultProvider = new StorageConfigurationDefaultProvider();
    StorageConfiguration storageConfiguration = new StorageConfiguration();
    String[] argv = new String[]{
        StorageConfiguration.STORAGE_DISPATCHER_CAPACITY_FLAG,
        "100",
        StorageConfiguration.STORAGE_STORE_DIRECTORY_PATH_FLAG,
        "~/random/relative/path"};
    // Act
    IllegalConfigurationException exception =
        assertThrows(IllegalConfigurationException.class,
            () -> JCommander.newBuilder()
                .addObject(storageConfiguration)
                .defaultProvider(defaultProvider)
                .build()
                .parse(argv));
    // Assert
    assertTrue(
        exception.getMessage().contains(StorageConfiguration.STORAGE_STORE_DIRECTORY_PATH_FLAG));
  }
}
