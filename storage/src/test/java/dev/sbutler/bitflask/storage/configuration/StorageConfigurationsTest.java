package dev.sbutler.bitflask.storage.configuration;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.beust.jcommander.JCommander;
import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.common.configuration.ConfigurationDefaultProvider;
import dev.sbutler.bitflask.common.configuration.exceptions.IllegalConfigurationException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class StorageConfigurationsTest {

  private static final ImmutableList<String> CLI_ARG_LIST =
      ImmutableList.of(
          StorageConfigurationsConstants.DISPATCHER_CAPACITY_FLAG,
          "100",
          StorageConfigurationsConstants.STORE_DIRECTORY_PATH_FLAG,
          Path.of("/tmp/random/absolute/path").toString(),
          StorageConfigurationsConstants.LOADING_MODE_FLAG,
          "load",
          StorageConfigurationsConstants.MEMTABLE_FLUSH_THRESHOLD_BYTES_FLAG,
          "1",
          StorageConfigurationsConstants.SEGMENT_LEVEL_COMPACT_THRESHOLD_BYTES_FLAG,
          "5");

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
    assertThat(storageConfigurations.getDispatcherCapacity())
        .isEqualTo(Integer.parseInt(defaultProvider.getDefaultValueFor(
            StorageConfigurationsConstants.DISPATCHER_CAPACITY_FLAG)));
    assertThat(storageConfigurations.getStoreDirectoryPath().toString())
        .isEqualTo(Path.of(defaultProvider.getDefaultValueFor(
            StorageConfigurationsConstants.STORE_DIRECTORY_PATH_FLAG)).toString());
    assertThat(storageConfigurations.getStorageLoadingMode())
        .isEqualTo(StorageLoadingMode.LOAD);
    assertThat(storageConfigurations.getMemtableFlushThresholdBytes())
        .isEqualTo(Integer.parseInt(defaultProvider.getDefaultValueFor(
            StorageConfigurationsConstants.MEMTABLE_FLUSH_THRESHOLD_BYTES_FLAG)));
    assertThat(storageConfigurations.getSegmentLevelFlushThresholdBytes())
        .isEqualTo(Integer.parseInt(defaultProvider.getDefaultValueFor(
            StorageConfigurationsConstants.SEGMENT_LEVEL_COMPACT_THRESHOLD_BYTES_FLAG)));
  }

  @Test
  void propertyFile_dispatcherCapacity_throwsIllegalConfigurationException() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = mock(ConfigurationDefaultProvider.class);
    mockDefaultProvider(defaultProvider);
    when(defaultProvider
        .getDefaultValueFor(StorageConfigurationsConstants.DISPATCHER_CAPACITY_FLAG))
        .thenReturn("-1");

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
        .contains(StorageConfigurationsConstants.DISPATCHER_CAPACITY_FLAG);
  }

  @Test
  void propertyFile_storeDirectoryFlag_throwsIllegalConfigurationException() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = mock(ConfigurationDefaultProvider.class);
    mockDefaultProvider(defaultProvider);
    when(defaultProvider
        .getDefaultValueFor(StorageConfigurationsConstants.STORE_DIRECTORY_PATH_FLAG))
        .thenReturn("~/.bitflask");

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
        .contains(StorageConfigurationsConstants.STORE_DIRECTORY_PATH_FLAG);
  }

  @Test
  void propertyFile_storageLoadingMode_throwsIllegalConfigurationException() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = mock(ConfigurationDefaultProvider.class);
    mockDefaultProvider(defaultProvider);
    when(defaultProvider
        .getDefaultValueFor(StorageConfigurationsConstants.LOADING_MODE_FLAG))
        .thenReturn("append");

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
        .contains(StorageConfigurationsConstants.LOADING_MODE_FLAG);
  }

  @Test
  void propertyFile_memtableFlushThresholdMB_throwsIllegalConfigurationException() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = mock(ConfigurationDefaultProvider.class);
    mockDefaultProvider(defaultProvider);
    when(defaultProvider
        .getDefaultValueFor(StorageConfigurationsConstants.MEMTABLE_FLUSH_THRESHOLD_BYTES_FLAG))
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
        .contains(StorageConfigurationsConstants.MEMTABLE_FLUSH_THRESHOLD_BYTES_FLAG);
  }

  @Test
  void propertyFile_segmentLevelCompactThresholdMB_throwsIllegalConfigurationException() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = mock(ConfigurationDefaultProvider.class);
    mockDefaultProvider(defaultProvider);
    when(defaultProvider
        .getDefaultValueFor(
            StorageConfigurationsConstants.SEGMENT_LEVEL_COMPACT_THRESHOLD_BYTES_FLAG))
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
        .contains(StorageConfigurationsConstants.SEGMENT_LEVEL_COMPACT_THRESHOLD_BYTES_FLAG);
  }

  @Test
  void commandLineFlags() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = new ConfigurationDefaultProvider(
        StorageConfigurationsConstants.STORAGE_FLAG_TO_CONFIGURATION_MAP);
    StorageConfigurations storageConfigurations = new StorageConfigurations();

    String[] argv = CLI_ARG_LIST.toArray(new String[0]);
    // Act
    JCommander.newBuilder()
        .addObject(storageConfigurations)
        .defaultProvider(defaultProvider)
        .build()
        .parse(argv);
    // Assert
    assertThat(storageConfigurations.getDispatcherCapacity()).isEqualTo(100);
    assertThat(storageConfigurations.getStoreDirectoryPath().toString())
        .isEqualTo(CLI_ARG_LIST.get(3));
    assertThat(storageConfigurations.getStorageLoadingMode())
        .isEqualTo(StorageLoadingMode.LOAD);
    assertThat(storageConfigurations.getMemtableFlushThresholdBytes())
        .isEqualTo(1);
    assertThat(storageConfigurations.getSegmentLevelFlushThresholdBytes())
        .isEqualTo(5);
  }

  @Test
  void commandLineFlags_dispatcherCapacity_throwsIllegalConfigurationException() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = new ConfigurationDefaultProvider(
        StorageConfigurationsConstants.STORAGE_FLAG_TO_CONFIGURATION_MAP);
    StorageConfigurations storageConfigurations = new StorageConfigurations();

    String[] argv = CLI_ARG_LIST.toArray(new String[0]);
    argv[1] = "-1";

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
        .contains(StorageConfigurationsConstants.DISPATCHER_CAPACITY_FLAG);
  }

  @Test
  void commandLineFlags_storeDirectoryFlag_throwsIllegalConfigurationException() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = new ConfigurationDefaultProvider(
        StorageConfigurationsConstants.STORAGE_FLAG_TO_CONFIGURATION_MAP);
    StorageConfigurations storageConfigurations = new StorageConfigurations();

    String[] argv = CLI_ARG_LIST.toArray(new String[0]);
    argv[3] = "~/random/relative/path";

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
        .contains(StorageConfigurationsConstants.STORE_DIRECTORY_PATH_FLAG);
  }

  @Test
  void commandLineFlags_storageLoadingMode_throwsIllegalConfigurationException() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = new ConfigurationDefaultProvider(
        StorageConfigurationsConstants.STORAGE_FLAG_TO_CONFIGURATION_MAP);
    StorageConfigurations storageConfigurations = new StorageConfigurations();

    String[] argv = CLI_ARG_LIST.toArray(new String[0]);
    argv[5] = "append";

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
        .contains(StorageConfigurationsConstants.LOADING_MODE_FLAG);
  }

  @Test
  void commandLineFlags_memtableFlushThresholdMB_throwsIllegalConfigurationException() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = new ConfigurationDefaultProvider(
        StorageConfigurationsConstants.STORAGE_FLAG_TO_CONFIGURATION_MAP);
    StorageConfigurations storageConfigurations = new StorageConfigurations();

    String[] argv = CLI_ARG_LIST.toArray(new String[0]);
    argv[7] = "-1";

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
        .contains(StorageConfigurationsConstants.MEMTABLE_FLUSH_THRESHOLD_BYTES_FLAG);
  }

  @Test
  void commandLineFlags_segmentLevelCompactThresholdMB_throwsIllegalConfigurationException() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = new ConfigurationDefaultProvider(
        StorageConfigurationsConstants.STORAGE_FLAG_TO_CONFIGURATION_MAP);
    StorageConfigurations storageConfigurations = new StorageConfigurations();

    String[] argv = CLI_ARG_LIST.toArray(new String[0]);
    argv[9] = "-1";

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
        .contains(StorageConfigurationsConstants.SEGMENT_LEVEL_COMPACT_THRESHOLD_BYTES_FLAG);
  }

  private static void mockDefaultProvider(ConfigurationDefaultProvider defaultProvider) {
    when(defaultProvider
        .getDefaultValueFor(StorageConfigurationsConstants.DISPATCHER_CAPACITY_FLAG))
        .thenReturn("1");
    when(defaultProvider
        .getDefaultValueFor(StorageConfigurationsConstants.STORE_DIRECTORY_PATH_FLAG))
        .thenReturn("/tmp/.bitflask");
    when(defaultProvider
        .getDefaultValueFor(StorageConfigurationsConstants.LOADING_MODE_FLAG))
        .thenReturn("load");
    when(defaultProvider
        .getDefaultValueFor(StorageConfigurationsConstants.MEMTABLE_FLUSH_THRESHOLD_BYTES_FLAG))
        .thenReturn("1");
    when(defaultProvider
        .getDefaultValueFor(
            StorageConfigurationsConstants.SEGMENT_LEVEL_COMPACT_THRESHOLD_BYTES_FLAG))
        .thenReturn("5");
  }
}
