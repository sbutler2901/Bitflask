package dev.sbutler.bitflask.storage.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.beust.jcommander.JCommander;
import dev.sbutler.bitflask.common.configuration.ConfigurationDefaultProvider;
import dev.sbutler.bitflask.common.configuration.exceptions.IllegalConfigurationException;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurationsConstants.StorageSegmentCreationModeArgs;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.junit.jupiter.api.Test;

public class StorageConfigurationsTest {

  @Test
  void propertyFile() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = new ConfigurationDefaultProvider(
        StorageConfigurationsConstants.STORAGE_FLAG_TO_CONFIGURATION_MAP);
    StorageConfigurations storageConfigurations = new StorageConfigurations();
    String[] argv = new String[]{};
    // Act
    JCommander.newBuilder()
        .addObject(storageConfigurations)
        .defaultProvider(defaultProvider)
        .build()
        .parse(argv);
    // Assert
    assertEquals(
        Integer.parseInt(defaultProvider.getDefaultValueFor(
            StorageConfigurationsConstants.STORAGE_DISPATCHER_CAPACITY_FLAG)),
        storageConfigurations.getStorageDispatcherCapacity());
    assertEquals(
        Path.of(defaultProvider.getDefaultValueFor(
            StorageConfigurationsConstants.STORAGE_STORE_DIRECTORY_PATH_FLAG)),
        storageConfigurations.getStorageStoreDirectoryPath());
    assertEquals(Long.parseLong(defaultProvider.getDefaultValueFor(
            StorageConfigurationsConstants.STORAGE_SEGMENT_SIZE_LIMIT_FLAG)),
        storageConfigurations.getStorageSegmentSizeLimit());
    assertEquals(StandardOpenOption.CREATE, storageConfigurations.getStorageSegmentCreationMode());
    assertEquals(Integer.parseInt(defaultProvider.getDefaultValueFor(
            StorageConfigurationsConstants.STORAGE_COMPACTION_THRESHOLD_FLAG)),
        storageConfigurations.getStorageCompactionThreshold());
  }

  @Test
  void propertyFile_illegalConfiguration_storageDispatcherCapacity() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = mock(
        ConfigurationDefaultProvider.class);
    doReturn("-1").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_DISPATCHER_CAPACITY_FLAG);
    doReturn("/tmp/.bitflask").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_STORE_DIRECTORY_PATH_FLAG);
    doReturn("100").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_SEGMENT_SIZE_LIMIT_FLAG);
    doReturn(StorageSegmentCreationModeArgs.CREATE.getRawArg()).when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_SEGMENT_CREATION_MODE_FLAG);
    doReturn("1").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_COMPACTION_THRESHOLD_FLAG);

    StorageConfigurations storageConfigurations = new StorageConfigurations();
    String[] argv = new String[]{};
    // Act
    IllegalConfigurationException exception =
        assertThrows(IllegalConfigurationException.class,
            () -> JCommander.newBuilder()
                .addObject(storageConfigurations)
                .defaultProvider(defaultProvider)
                .build()
                .parse(argv));
    // Assert
    assertTrue(
        exception.getMessage()
            .contains(StorageConfigurationsConstants.STORAGE_DISPATCHER_CAPACITY_FLAG));
  }

  @Test
  void propertyFile_illegalConfiguration_storeDirectoryFlag() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = mock(
        ConfigurationDefaultProvider.class);
    doReturn("1").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_DISPATCHER_CAPACITY_FLAG);
    doReturn("~/.bitflask").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_STORE_DIRECTORY_PATH_FLAG);
    doReturn("100").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_SEGMENT_SIZE_LIMIT_FLAG);
    doReturn(StorageSegmentCreationModeArgs.CREATE.getRawArg()).when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_SEGMENT_CREATION_MODE_FLAG);
    doReturn("1").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_COMPACTION_THRESHOLD_FLAG);

    StorageConfigurations storageConfigurations = new StorageConfigurations();
    String[] argv = new String[]{};
    // Act
    IllegalConfigurationException exception =
        assertThrows(IllegalConfigurationException.class, () ->
            JCommander.newBuilder()
                .addObject(storageConfigurations)
                .defaultProvider(defaultProvider)
                .build()
                .parse(argv));
    // Assert
    assertTrue(
        exception.getMessage()
            .contains(StorageConfigurationsConstants.STORAGE_STORE_DIRECTORY_PATH_FLAG));
  }

  @Test
  void propertyFile_illegalConfiguration_segmentSizeLimit() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = mock(
        ConfigurationDefaultProvider.class);
    doReturn("1").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_DISPATCHER_CAPACITY_FLAG);
    doReturn("/tmp/.bitflask").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_STORE_DIRECTORY_PATH_FLAG);
    doReturn("-1").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_SEGMENT_SIZE_LIMIT_FLAG);
    doReturn(StorageSegmentCreationModeArgs.CREATE.getRawArg()).when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_SEGMENT_CREATION_MODE_FLAG);
    doReturn("1").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_COMPACTION_THRESHOLD_FLAG);

    StorageConfigurations storageConfigurations = new StorageConfigurations();
    String[] argv = new String[]{};
    // Act
    IllegalConfigurationException exception =
        assertThrows(IllegalConfigurationException.class, () ->
            JCommander.newBuilder()
                .addObject(storageConfigurations)
                .defaultProvider(defaultProvider)
                .build()
                .parse(argv));
    // Assert
    assertTrue(
        exception.getMessage()
            .contains(StorageConfigurationsConstants.STORAGE_SEGMENT_SIZE_LIMIT_FLAG));
  }

  @Test
  void propertyFile_illegalConfiguration_storageSegmentCreationMode() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = mock(
        ConfigurationDefaultProvider.class);
    doReturn("1").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_DISPATCHER_CAPACITY_FLAG);
    doReturn("/tmp/.bitflask").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_STORE_DIRECTORY_PATH_FLAG);
    doReturn("100").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_SEGMENT_SIZE_LIMIT_FLAG);
    doReturn("append").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_SEGMENT_CREATION_MODE_FLAG);
    doReturn("1").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_COMPACTION_THRESHOLD_FLAG);

    StorageConfigurations storageConfigurations = new StorageConfigurations();
    String[] argv = new String[]{};
    // Act
    IllegalConfigurationException exception =
        assertThrows(IllegalConfigurationException.class, () ->
            JCommander.newBuilder()
                .addObject(storageConfigurations)
                .defaultProvider(defaultProvider)
                .build()
                .parse(argv));
    // Assert
    assertTrue(
        exception.getMessage()
            .contains(StorageConfigurationsConstants.STORAGE_SEGMENT_CREATION_MODE_NAME));
  }

  @Test
  void propertyFile_illegalConfiguration_compactionThreshold() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = mock(
        ConfigurationDefaultProvider.class);
    doReturn("1").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_DISPATCHER_CAPACITY_FLAG);
    doReturn("/tmp/.bitflask").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_STORE_DIRECTORY_PATH_FLAG);
    doReturn("100").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_SEGMENT_SIZE_LIMIT_FLAG);
    doReturn(StorageSegmentCreationModeArgs.CREATE.getRawArg()).when(
            defaultProvider)
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_SEGMENT_CREATION_MODE_FLAG);
    doReturn("-1").when(defaultProvider)
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_COMPACTION_THRESHOLD_FLAG);

    StorageConfigurations storageConfigurations = new StorageConfigurations();
    String[] argv = new String[]{};
    // Act
    IllegalConfigurationException exception =
        assertThrows(IllegalConfigurationException.class, () ->
            JCommander.newBuilder()
                .addObject(storageConfigurations)
                .defaultProvider(defaultProvider)
                .build()
                .parse(argv));
    // Assert
    assertTrue(
        exception.getMessage()
            .contains(StorageConfigurationsConstants.STORAGE_COMPACTION_THRESHOLD_FLAG));
  }

  @Test
  void commandLineFlags() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = new ConfigurationDefaultProvider(
        StorageConfigurationsConstants.STORAGE_FLAG_TO_CONFIGURATION_MAP);
    StorageConfigurations storageConfigurations = new StorageConfigurations();
    Path expectedSegmentDirPath = Paths.get("/random/absolute/path");
    String[] argv = new String[]{
        StorageConfigurationsConstants.STORAGE_DISPATCHER_CAPACITY_FLAG,
        "100",
        StorageConfigurationsConstants.STORAGE_STORE_DIRECTORY_PATH_FLAG,
        expectedSegmentDirPath.toString(),
        StorageConfigurationsConstants.STORAGE_SEGMENT_SIZE_LIMIT_FLAG,
        "200",
        StorageConfigurationsConstants.STORAGE_SEGMENT_CREATION_MODE_FLAG,
        StorageSegmentCreationModeArgs.TRUNCATE.getRawArg(),
        StorageConfigurationsConstants.STORAGE_COMPACTION_THRESHOLD_FLAG,
        "1",
    };
    // Act
    JCommander.newBuilder()
        .addObject(storageConfigurations)
        .defaultProvider(defaultProvider)
        .build()
        .parse(argv);
    // Assert
    assertEquals(100, storageConfigurations.getStorageDispatcherCapacity());
    assertEquals(expectedSegmentDirPath, storageConfigurations.getStorageStoreDirectoryPath());
    assertEquals(200, storageConfigurations.getStorageSegmentSizeLimit());
    assertEquals(1, storageConfigurations.getStorageCompactionThreshold());
  }

  @Test
  void commandLineFlags_illegalConfiguration_storageDispatcherCapacity() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = new ConfigurationDefaultProvider(
        StorageConfigurationsConstants.STORAGE_FLAG_TO_CONFIGURATION_MAP);
    StorageConfigurations storageConfigurations = new StorageConfigurations();
    String[] argv = new String[]{
        StorageConfigurationsConstants.STORAGE_DISPATCHER_CAPACITY_FLAG,
        "-1",
        StorageConfigurationsConstants.STORAGE_STORE_DIRECTORY_PATH_FLAG,
        "/random/absolute/path",
        StorageConfigurationsConstants.STORAGE_SEGMENT_SIZE_LIMIT_FLAG,
        "200",
        StorageConfigurationsConstants.STORAGE_SEGMENT_CREATION_MODE_FLAG,
        StorageSegmentCreationModeArgs.CREATE.getRawArg(),
        StorageConfigurationsConstants.STORAGE_COMPACTION_THRESHOLD_FLAG,
        "1",
    };
    // Act
    IllegalConfigurationException exception =
        assertThrows(IllegalConfigurationException.class,
            () -> JCommander.newBuilder()
                .addObject(storageConfigurations)
                .defaultProvider(defaultProvider)
                .build()
                .parse(argv));
    // Assert
    assertTrue(
        exception.getMessage()
            .contains(StorageConfigurationsConstants.STORAGE_DISPATCHER_CAPACITY_FLAG));
  }

  @Test
  void commandLineFlags_illegalConfiguration_storeDirectoryFlag() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = new ConfigurationDefaultProvider(
        StorageConfigurationsConstants.STORAGE_FLAG_TO_CONFIGURATION_MAP);
    StorageConfigurations storageConfigurations = new StorageConfigurations();
    String[] argv = new String[]{
        StorageConfigurationsConstants.STORAGE_DISPATCHER_CAPACITY_FLAG,
        "100",
        StorageConfigurationsConstants.STORAGE_STORE_DIRECTORY_PATH_FLAG,
        "~/random/relative/path",
        StorageConfigurationsConstants.STORAGE_SEGMENT_SIZE_LIMIT_FLAG,
        "200",
        StorageConfigurationsConstants.STORAGE_SEGMENT_CREATION_MODE_FLAG,
        StorageSegmentCreationModeArgs.CREATE.getRawArg(),
        StorageConfigurationsConstants.STORAGE_COMPACTION_THRESHOLD_FLAG,
        "1",
    };
    // Act
    IllegalConfigurationException exception =
        assertThrows(IllegalConfigurationException.class,
            () -> JCommander.newBuilder()
                .addObject(storageConfigurations)
                .defaultProvider(defaultProvider)
                .build()
                .parse(argv));
    // Assert
    assertTrue(
        exception.getMessage()
            .contains(StorageConfigurationsConstants.STORAGE_STORE_DIRECTORY_PATH_FLAG));
  }

  @Test
  void commandLineFlags_illegalConfiguration_segmentSizeLimitFlag() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = new ConfigurationDefaultProvider(
        StorageConfigurationsConstants.STORAGE_FLAG_TO_CONFIGURATION_MAP);
    StorageConfigurations storageConfigurations = new StorageConfigurations();
    String[] argv = new String[]{
        StorageConfigurationsConstants.STORAGE_DISPATCHER_CAPACITY_FLAG,
        "100",
        StorageConfigurationsConstants.STORAGE_STORE_DIRECTORY_PATH_FLAG,
        "/random/absolute/path",
        StorageConfigurationsConstants.STORAGE_SEGMENT_SIZE_LIMIT_FLAG,
        "-1",
        StorageConfigurationsConstants.STORAGE_SEGMENT_CREATION_MODE_FLAG,
        StorageSegmentCreationModeArgs.CREATE.getRawArg(),
        StorageConfigurationsConstants.STORAGE_COMPACTION_THRESHOLD_FLAG,
        "1",
    };
    // Act
    IllegalConfigurationException exception =
        assertThrows(IllegalConfigurationException.class,
            () -> JCommander.newBuilder()
                .addObject(storageConfigurations)
                .defaultProvider(defaultProvider)
                .build()
                .parse(argv));
    // Assert
    assertTrue(
        exception.getMessage()
            .contains(StorageConfigurationsConstants.STORAGE_SEGMENT_SIZE_LIMIT_FLAG));
  }

  @Test
  void commandLineFlags_illegalConfiguration_storageSegmentCreationMode() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = new ConfigurationDefaultProvider(
        StorageConfigurationsConstants.STORAGE_FLAG_TO_CONFIGURATION_MAP);
    StorageConfigurations storageConfigurations = new StorageConfigurations();
    String[] argv = new String[]{
        StorageConfigurationsConstants.STORAGE_DISPATCHER_CAPACITY_FLAG,
        "100",
        StorageConfigurationsConstants.STORAGE_STORE_DIRECTORY_PATH_FLAG,
        "/random/absolute/path",
        StorageConfigurationsConstants.STORAGE_SEGMENT_SIZE_LIMIT_FLAG,
        "200",
        StorageConfigurationsConstants.STORAGE_SEGMENT_CREATION_MODE_FLAG,
        "append",
        StorageConfigurationsConstants.STORAGE_COMPACTION_THRESHOLD_FLAG,
        "1",
    };
    // Act
    IllegalConfigurationException exception =
        assertThrows(IllegalConfigurationException.class,
            () -> JCommander.newBuilder()
                .addObject(storageConfigurations)
                .defaultProvider(defaultProvider)
                .build()
                .parse(argv));
    // Assert
    assertTrue(
        exception.getMessage()
            .contains(StorageConfigurationsConstants.STORAGE_SEGMENT_CREATION_MODE_NAME));
  }

  @Test
  void commandLineFlags_illegalConfiguration_segmentCompactionThresholdFlag() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = new ConfigurationDefaultProvider(
        StorageConfigurationsConstants.STORAGE_FLAG_TO_CONFIGURATION_MAP);
    StorageConfigurations storageConfigurations = new StorageConfigurations();
    String[] argv = new String[]{
        StorageConfigurationsConstants.STORAGE_DISPATCHER_CAPACITY_FLAG,
        "100",
        StorageConfigurationsConstants.STORAGE_STORE_DIRECTORY_PATH_FLAG,
        "/random/absolute/path",
        StorageConfigurationsConstants.STORAGE_SEGMENT_SIZE_LIMIT_FLAG,
        "200",
        StorageConfigurationsConstants.STORAGE_SEGMENT_CREATION_MODE_FLAG,
        StorageSegmentCreationModeArgs.CREATE.getRawArg(),
        StorageConfigurationsConstants.STORAGE_COMPACTION_THRESHOLD_FLAG,
        "-1",
    };
    // Act
    IllegalConfigurationException exception =
        assertThrows(IllegalConfigurationException.class,
            () -> JCommander.newBuilder()
                .addObject(storageConfigurations)
                .defaultProvider(defaultProvider)
                .build()
                .parse(argv));
    // Assert
    assertTrue(
        exception.getMessage()
            .contains(StorageConfigurationsConstants.STORAGE_COMPACTION_THRESHOLD_FLAG));
  }
}
