package dev.sbutler.bitflask.storage.configuration;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.beust.jcommander.JCommander;
import dev.sbutler.bitflask.common.configuration.ConfigurationDefaultProvider;
import dev.sbutler.bitflask.common.configuration.exceptions.IllegalConfigurationException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

public class StorageConfigurationsTest {

  @Test
  void propertyFile_defaults() {
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
    assertThat(storageConfigurations.getStorageDispatcherCapacity())
        .isEqualTo(Integer.parseInt(defaultProvider.getDefaultValueFor(
            StorageConfigurationsConstants.STORAGE_DISPATCHER_CAPACITY_FLAG)));
    assertThat(storageConfigurations.getStorageStoreDirectoryPath().toString())
        .isEqualTo(Path.of(defaultProvider.getDefaultValueFor(
            StorageConfigurationsConstants.STORAGE_STORE_DIRECTORY_PATH_FLAG)).toString());
    assertThat(storageConfigurations.getStorageSegmentSizeLimit())
        .isEqualTo(Long.parseLong(defaultProvider.getDefaultValueFor(
            StorageConfigurationsConstants.STORAGE_SEGMENT_SIZE_LIMIT_FLAG)));
    assertThat(storageConfigurations.getStorageLoadingMode())
        .isEqualTo(StorageLoadingMode.LOAD);
    assertThat(storageConfigurations.getStorageCompactionThreshold())
        .isEqualTo(Integer.parseInt(defaultProvider.getDefaultValueFor(
            StorageConfigurationsConstants.STORAGE_COMPACTION_THRESHOLD_FLAG)));
  }

  @Test
  void propertyFile_illegalConfiguration_storageDispatcherCapacity() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = mock(ConfigurationDefaultProvider.class);
    when(defaultProvider
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_DISPATCHER_CAPACITY_FLAG))
        .thenReturn("-1");
    when(defaultProvider
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_STORE_DIRECTORY_PATH_FLAG))
        .thenReturn("/tmp/.bitflask");
    when(defaultProvider
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_SEGMENT_SIZE_LIMIT_FLAG))
        .thenReturn("100");
    when(defaultProvider
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_LOADING_MODE_FLAG))
        .thenReturn("load");
    when(defaultProvider
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_COMPACTION_THRESHOLD_FLAG))
        .thenReturn("1");

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
    assertThat(exception).hasMessageThat()
        .contains(StorageConfigurationsConstants.STORAGE_DISPATCHER_CAPACITY_FLAG);
  }

  @Test
  void propertyFile_illegalConfiguration_storeDirectoryFlag() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = mock(ConfigurationDefaultProvider.class);
    when(defaultProvider
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_DISPATCHER_CAPACITY_FLAG))
        .thenReturn("1");
    when(defaultProvider
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_STORE_DIRECTORY_PATH_FLAG))
        .thenReturn("~/.bitflask");
    when(defaultProvider
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_SEGMENT_SIZE_LIMIT_FLAG))
        .thenReturn("100");
    when(defaultProvider
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_LOADING_MODE_FLAG))
        .thenReturn("load");
    when(defaultProvider
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_COMPACTION_THRESHOLD_FLAG))
        .thenReturn("1");

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
    assertThat(exception).hasMessageThat()
        .contains(StorageConfigurationsConstants.STORAGE_STORE_DIRECTORY_PATH_FLAG);
  }

  @Test
  void propertyFile_illegalConfiguration_segmentSizeLimit() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = mock(ConfigurationDefaultProvider.class);
    when(defaultProvider
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_DISPATCHER_CAPACITY_FLAG))
        .thenReturn("1");
    when(defaultProvider
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_STORE_DIRECTORY_PATH_FLAG))
        .thenReturn("/tmp/.bitflask");
    when(defaultProvider
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_SEGMENT_SIZE_LIMIT_FLAG))
        .thenReturn("-1");
    when(defaultProvider
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_LOADING_MODE_FLAG))
        .thenReturn("load");
    when(defaultProvider
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_COMPACTION_THRESHOLD_FLAG))
        .thenReturn("1");

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
    assertThat(exception).hasMessageThat()
        .contains(StorageConfigurationsConstants.STORAGE_SEGMENT_SIZE_LIMIT_FLAG);
  }

  @Test
  void propertyFile_illegalConfiguration_storageLoadingMode() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = mock(ConfigurationDefaultProvider.class);
    when(defaultProvider
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_DISPATCHER_CAPACITY_FLAG))
        .thenReturn("1");
    when(defaultProvider
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_STORE_DIRECTORY_PATH_FLAG))
        .thenReturn("/tmp/.bitflask");
    when(defaultProvider
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_SEGMENT_SIZE_LIMIT_FLAG))
        .thenReturn("100");
    when(defaultProvider
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_LOADING_MODE_FLAG))
        .thenReturn("append");
    when(defaultProvider
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_COMPACTION_THRESHOLD_FLAG))
        .thenReturn("1");

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
    assertThat(exception).hasMessageThat()
        .contains(StorageConfigurationsConstants.STORAGE_LOADING_MODE_FLAG);
  }

  @Test
  void propertyFile_illegalConfiguration_compactionThreshold() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = mock(ConfigurationDefaultProvider.class);
    when(defaultProvider
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_DISPATCHER_CAPACITY_FLAG))
        .thenReturn("1");
    when(defaultProvider
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_STORE_DIRECTORY_PATH_FLAG))
        .thenReturn("/tmp/.bitflask");
    when(defaultProvider
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_SEGMENT_SIZE_LIMIT_FLAG))
        .thenReturn("100");
    when(defaultProvider
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_LOADING_MODE_FLAG))
        .thenReturn("load");
    when(defaultProvider
        .getDefaultValueFor(StorageConfigurationsConstants.STORAGE_COMPACTION_THRESHOLD_FLAG))
        .thenReturn("-1");

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
    assertThat(exception).hasMessageThat()
        .contains(StorageConfigurationsConstants.STORAGE_COMPACTION_THRESHOLD_FLAG);
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
        StorageConfigurationsConstants.STORAGE_LOADING_MODE_FLAG,
        "load",
        StorageConfigurationsConstants.STORAGE_COMPACTION_THRESHOLD_FLAG,
        "1"};
    // Act
    JCommander.newBuilder()
        .addObject(storageConfigurations)
        .defaultProvider(defaultProvider)
        .build()
        .parse(argv);
    // Assert
    assertThat(storageConfigurations.getStorageDispatcherCapacity()).isEqualTo(100);
    assertThat(storageConfigurations.getStorageStoreDirectoryPath().toString())
        .isEqualTo(expectedSegmentDirPath.toString());
    assertThat(storageConfigurations.getStorageSegmentSizeLimit()).isEqualTo(200);
    assertThat(storageConfigurations.getStorageCompactionThreshold()).isEqualTo(1);
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
        StorageConfigurationsConstants.STORAGE_LOADING_MODE_FLAG,
        "load",
        StorageConfigurationsConstants.STORAGE_COMPACTION_THRESHOLD_FLAG,
        "1"};
    // Act
    IllegalConfigurationException exception =
        assertThrows(IllegalConfigurationException.class,
            () -> JCommander.newBuilder()
                .addObject(storageConfigurations)
                .defaultProvider(defaultProvider)
                .build()
                .parse(argv));
    // Assert
    assertThat(exception).hasMessageThat()
        .contains(StorageConfigurationsConstants.STORAGE_DISPATCHER_CAPACITY_FLAG);
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
        StorageConfigurationsConstants.STORAGE_LOADING_MODE_FLAG,
        "load",
        StorageConfigurationsConstants.STORAGE_COMPACTION_THRESHOLD_FLAG,
        "1"};
    // Act
    IllegalConfigurationException exception =
        assertThrows(IllegalConfigurationException.class,
            () -> JCommander.newBuilder()
                .addObject(storageConfigurations)
                .defaultProvider(defaultProvider)
                .build()
                .parse(argv));
    // Assert
    assertThat(exception).hasMessageThat()
        .contains(StorageConfigurationsConstants.STORAGE_STORE_DIRECTORY_PATH_FLAG);
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
        StorageConfigurationsConstants.STORAGE_LOADING_MODE_FLAG,
        "load",
        StorageConfigurationsConstants.STORAGE_COMPACTION_THRESHOLD_FLAG,
        "1"};
    // Act
    IllegalConfigurationException exception =
        assertThrows(IllegalConfigurationException.class,
            () -> JCommander.newBuilder()
                .addObject(storageConfigurations)
                .defaultProvider(defaultProvider)
                .build()
                .parse(argv));
    // Assert
    assertThat(exception).hasMessageThat()
        .contains(StorageConfigurationsConstants.STORAGE_SEGMENT_SIZE_LIMIT_FLAG);
  }

  @Test
  void commandLineFlags_illegalConfiguration_storageLoadingMode() {
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
        StorageConfigurationsConstants.STORAGE_LOADING_MODE_FLAG,
        "append",
        StorageConfigurationsConstants.STORAGE_COMPACTION_THRESHOLD_FLAG,
        "1"};
    // Act
    IllegalConfigurationException exception =
        assertThrows(IllegalConfigurationException.class,
            () -> JCommander.newBuilder()
                .addObject(storageConfigurations)
                .defaultProvider(defaultProvider)
                .build()
                .parse(argv));
    // Assert
    assertThat(exception).hasMessageThat()
        .contains(StorageConfigurationsConstants.STORAGE_LOADING_MODE_FLAG);
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
        StorageConfigurationsConstants.STORAGE_LOADING_MODE_FLAG,
        "load",
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
    assertThat(exception).hasMessageThat()
        .contains(StorageConfigurationsConstants.STORAGE_COMPACTION_THRESHOLD_FLAG);
  }
}
