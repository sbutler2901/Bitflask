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

  private static final Path STORE_DIRECTORY_PATH_DEFAULT = Path.of("/tmp/.bitflask");
  private static final StorageLoadingMode LOADING_MODE_DEFAULT = StorageLoadingMode.LOAD;
  private static final long MEMTABLE_FLUSH_BYTES_DEFAULT = 1;
  private static final long SEGMENT_LEVEL_FLUSH_BYTES_DEFAULT = 5;
  private static final long COMPACTOR_DELAY_DEFAULT = 5000;

  private static final ImmutableList<String> CLI_ARG_LIST =
      ImmutableList.of(
          StorageConfigurationsConstants.STORE_DIRECTORY_PATH_FLAG,
          STORE_DIRECTORY_PATH_DEFAULT.toString(),
          StorageConfigurationsConstants.LOADING_MODE_FLAG,
          LOADING_MODE_DEFAULT.toString(),
          StorageConfigurationsConstants.MEMTABLE_FLUSH_THRESHOLD_BYTES_FLAG,
          String.valueOf(MEMTABLE_FLUSH_BYTES_DEFAULT),
          StorageConfigurationsConstants.SEGMENT_LEVEL_COMPACT_THRESHOLD_BYTES_FLAG,
          String.valueOf(SEGMENT_LEVEL_FLUSH_BYTES_DEFAULT),
          StorageConfigurationsConstants.COMPACTOR_EXEC_DELAY_MILLISECONDS_FLAG,
          String.valueOf(COMPACTOR_DELAY_DEFAULT));

  @Test
  void propertyFile_defaults() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider =
        new ConfigurationDefaultProvider(
            StorageConfigurationsConstants.STORAGE_FLAG_TO_CONFIGURATION_MAP);
    StorageConfigurations storageConfigurations = new StorageConfigurations();
    String[] argv = new String[] {};
    // Act
    JCommander.newBuilder()
        .addObject(storageConfigurations)
        .defaultProvider(defaultProvider)
        .build()
        .parse(argv);
    // Assert
    assertThat(storageConfigurations.getStoreDirectoryPath().toString())
        .isEqualTo(
            Path.of(
                    defaultProvider.getDefaultValueFor(
                        StorageConfigurationsConstants.STORE_DIRECTORY_PATH_FLAG))
                .toString());
    assertThat(storageConfigurations.getStorageLoadingMode()).isEqualTo(StorageLoadingMode.LOAD);
    assertThat(storageConfigurations.getMemtableFlushThresholdBytes())
        .isEqualTo(
            Long.parseLong(
                defaultProvider.getDefaultValueFor(
                    StorageConfigurationsConstants.MEMTABLE_FLUSH_THRESHOLD_BYTES_FLAG)));
    assertThat(storageConfigurations.getSegmentLevelFlushThresholdBytes())
        .isEqualTo(
            Long.parseLong(
                defaultProvider.getDefaultValueFor(
                    StorageConfigurationsConstants.SEGMENT_LEVEL_COMPACT_THRESHOLD_BYTES_FLAG)));
    assertThat(storageConfigurations.getCompactorExecDelayMilliseconds())
        .isEqualTo(
            Long.parseLong(
                defaultProvider.getDefaultValueFor(
                    StorageConfigurationsConstants.COMPACTOR_EXEC_DELAY_MILLISECONDS_FLAG)));
  }

  @Test
  void propertyFile_storeDirectoryFlag_throwsIllegalConfigurationException() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = mock(ConfigurationDefaultProvider.class);
    mockDefaultProvider(defaultProvider);
    when(defaultProvider.getDefaultValueFor(
            StorageConfigurationsConstants.STORE_DIRECTORY_PATH_FLAG))
        .thenReturn("~/.bitflask");

    StorageConfigurations storageConfigurations = new StorageConfigurations();
    String[] argv = new String[] {};
    // Act
    IllegalConfigurationException exception =
        assertThrows(
            IllegalConfigurationException.class,
            () ->
                JCommander.newBuilder()
                    .addObject(storageConfigurations)
                    .defaultProvider(defaultProvider)
                    .build()
                    .parse(argv));
    // Assert
    assertThat(exception)
        .hasMessageThat()
        .contains(StorageConfigurationsConstants.STORE_DIRECTORY_PATH_FLAG);
  }

  @Test
  void propertyFile_storageLoadingMode_throwsIllegalConfigurationException() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = mock(ConfigurationDefaultProvider.class);
    mockDefaultProvider(defaultProvider);
    when(defaultProvider.getDefaultValueFor(StorageConfigurationsConstants.LOADING_MODE_FLAG))
        .thenReturn("append");

    StorageConfigurations storageConfigurations = new StorageConfigurations();
    String[] argv = new String[] {};
    // Act
    IllegalConfigurationException exception =
        assertThrows(
            IllegalConfigurationException.class,
            () ->
                JCommander.newBuilder()
                    .addObject(storageConfigurations)
                    .defaultProvider(defaultProvider)
                    .build()
                    .parse(argv));
    // Assert
    assertThat(exception)
        .hasMessageThat()
        .contains(StorageConfigurationsConstants.LOADING_MODE_FLAG);
  }

  @Test
  void propertyFile_memtableFlushThresholdMB_throwsIllegalConfigurationException() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = mock(ConfigurationDefaultProvider.class);
    mockDefaultProvider(defaultProvider);
    when(defaultProvider.getDefaultValueFor(
            StorageConfigurationsConstants.MEMTABLE_FLUSH_THRESHOLD_BYTES_FLAG))
        .thenReturn("-1");

    StorageConfigurations storageConfigurations = new StorageConfigurations();
    String[] argv = new String[] {};
    // Act
    IllegalConfigurationException exception =
        assertThrows(
            IllegalConfigurationException.class,
            () ->
                JCommander.newBuilder()
                    .addObject(storageConfigurations)
                    .defaultProvider(defaultProvider)
                    .build()
                    .parse(argv));
    // Assert
    assertThat(exception)
        .hasMessageThat()
        .contains(StorageConfigurationsConstants.MEMTABLE_FLUSH_THRESHOLD_BYTES_FLAG);
  }

  @Test
  void propertyFile_segmentLevelCompactThresholdMB_throwsIllegalConfigurationException() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = mock(ConfigurationDefaultProvider.class);
    mockDefaultProvider(defaultProvider);
    when(defaultProvider.getDefaultValueFor(
            StorageConfigurationsConstants.SEGMENT_LEVEL_COMPACT_THRESHOLD_BYTES_FLAG))
        .thenReturn("-1");

    StorageConfigurations storageConfigurations = new StorageConfigurations();
    String[] argv = new String[] {};
    // Act
    IllegalConfigurationException exception =
        assertThrows(
            IllegalConfigurationException.class,
            () ->
                JCommander.newBuilder()
                    .addObject(storageConfigurations)
                    .defaultProvider(defaultProvider)
                    .build()
                    .parse(argv));
    // Assert
    assertThat(exception)
        .hasMessageThat()
        .contains(StorageConfigurationsConstants.SEGMENT_LEVEL_COMPACT_THRESHOLD_BYTES_FLAG);
  }

  @Test
  void propertyFile_compactorExecDelayMilliseconds_throwsIllegalConfigurationException() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider = mock(ConfigurationDefaultProvider.class);
    mockDefaultProvider(defaultProvider);
    when(defaultProvider.getDefaultValueFor(
            StorageConfigurationsConstants.COMPACTOR_EXEC_DELAY_MILLISECONDS_FLAG))
        .thenReturn("-1");

    StorageConfigurations storageConfigurations = new StorageConfigurations();
    String[] argv = new String[] {};
    // Act
    IllegalConfigurationException exception =
        assertThrows(
            IllegalConfigurationException.class,
            () ->
                JCommander.newBuilder()
                    .addObject(storageConfigurations)
                    .defaultProvider(defaultProvider)
                    .build()
                    .parse(argv));
    // Assert
    assertThat(exception)
        .hasMessageThat()
        .contains(StorageConfigurationsConstants.COMPACTOR_EXEC_DELAY_MILLISECONDS_FLAG);
  }

  @Test
  void commandLineFlags() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider =
        new ConfigurationDefaultProvider(
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
    assertThat(storageConfigurations.getStoreDirectoryPath().toString())
        .isEqualTo(STORE_DIRECTORY_PATH_DEFAULT.toString());
    assertThat(storageConfigurations.getStorageLoadingMode()).isEqualTo(LOADING_MODE_DEFAULT);
    assertThat(storageConfigurations.getMemtableFlushThresholdBytes())
        .isEqualTo(MEMTABLE_FLUSH_BYTES_DEFAULT);
    assertThat(storageConfigurations.getSegmentLevelFlushThresholdBytes())
        .isEqualTo(SEGMENT_LEVEL_FLUSH_BYTES_DEFAULT);
    assertThat(storageConfigurations.getCompactorExecDelayMilliseconds())
        .isEqualTo(COMPACTOR_DELAY_DEFAULT);
  }

  @Test
  void commandLineFlags_storeDirectoryFlag_throwsIllegalConfigurationException() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider =
        new ConfigurationDefaultProvider(
            StorageConfigurationsConstants.STORAGE_FLAG_TO_CONFIGURATION_MAP);
    StorageConfigurations storageConfigurations = new StorageConfigurations();

    String[] argv = CLI_ARG_LIST.toArray(new String[0]);
    argv[1] = "~/random/relative/path";

    // Act
    IllegalConfigurationException exception =
        assertThrows(
            IllegalConfigurationException.class,
            () ->
                JCommander.newBuilder()
                    .addObject(storageConfigurations)
                    .defaultProvider(defaultProvider)
                    .build()
                    .parse(argv));
    // Assert
    assertThat(exception)
        .hasMessageThat()
        .contains(StorageConfigurationsConstants.STORE_DIRECTORY_PATH_FLAG);
  }

  @Test
  void commandLineFlags_storageLoadingMode_throwsIllegalConfigurationException() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider =
        new ConfigurationDefaultProvider(
            StorageConfigurationsConstants.STORAGE_FLAG_TO_CONFIGURATION_MAP);
    StorageConfigurations storageConfigurations = new StorageConfigurations();

    String[] argv = CLI_ARG_LIST.toArray(new String[0]);
    argv[3] = "append";

    // Act
    IllegalConfigurationException exception =
        assertThrows(
            IllegalConfigurationException.class,
            () ->
                JCommander.newBuilder()
                    .addObject(storageConfigurations)
                    .defaultProvider(defaultProvider)
                    .build()
                    .parse(argv));
    // Assert
    assertThat(exception)
        .hasMessageThat()
        .contains(StorageConfigurationsConstants.LOADING_MODE_FLAG);
  }

  @Test
  void commandLineFlags_memtableFlushThresholdMB_throwsIllegalConfigurationException() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider =
        new ConfigurationDefaultProvider(
            StorageConfigurationsConstants.STORAGE_FLAG_TO_CONFIGURATION_MAP);
    StorageConfigurations storageConfigurations = new StorageConfigurations();

    String[] argv = CLI_ARG_LIST.toArray(new String[0]);
    argv[5] = "-1";

    // Act
    IllegalConfigurationException exception =
        assertThrows(
            IllegalConfigurationException.class,
            () ->
                JCommander.newBuilder()
                    .addObject(storageConfigurations)
                    .defaultProvider(defaultProvider)
                    .build()
                    .parse(argv));
    // Assert
    assertThat(exception)
        .hasMessageThat()
        .contains(StorageConfigurationsConstants.MEMTABLE_FLUSH_THRESHOLD_BYTES_FLAG);
  }

  @Test
  void commandLineFlags_segmentLevelCompactThresholdMB_throwsIllegalConfigurationException() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider =
        new ConfigurationDefaultProvider(
            StorageConfigurationsConstants.STORAGE_FLAG_TO_CONFIGURATION_MAP);
    StorageConfigurations storageConfigurations = new StorageConfigurations();

    String[] argv = CLI_ARG_LIST.toArray(new String[0]);
    argv[7] = "-1";

    // Act
    IllegalConfigurationException exception =
        assertThrows(
            IllegalConfigurationException.class,
            () ->
                JCommander.newBuilder()
                    .addObject(storageConfigurations)
                    .defaultProvider(defaultProvider)
                    .build()
                    .parse(argv));
    // Assert
    assertThat(exception)
        .hasMessageThat()
        .contains(StorageConfigurationsConstants.SEGMENT_LEVEL_COMPACT_THRESHOLD_BYTES_FLAG);
  }

  @Test
  void commandLineFlags_compactorExecDelayMilliseconds_throwsIllegalConfigurationException() {
    // Arrange
    ConfigurationDefaultProvider defaultProvider =
        new ConfigurationDefaultProvider(
            StorageConfigurationsConstants.STORAGE_FLAG_TO_CONFIGURATION_MAP);
    StorageConfigurations storageConfigurations = new StorageConfigurations();

    String[] argv = CLI_ARG_LIST.toArray(new String[0]);
    argv[9] = "-1";

    // Act
    IllegalConfigurationException exception =
        assertThrows(
            IllegalConfigurationException.class,
            () ->
                JCommander.newBuilder()
                    .addObject(storageConfigurations)
                    .defaultProvider(defaultProvider)
                    .build()
                    .parse(argv));
    // Assert
    assertThat(exception)
        .hasMessageThat()
        .contains(StorageConfigurationsConstants.COMPACTOR_EXEC_DELAY_MILLISECONDS_FLAG);
  }

  private static void mockDefaultProvider(ConfigurationDefaultProvider defaultProvider) {
    when(defaultProvider.getDefaultValueFor(
            StorageConfigurationsConstants.STORE_DIRECTORY_PATH_FLAG))
        .thenReturn(STORE_DIRECTORY_PATH_DEFAULT.toString());
    when(defaultProvider.getDefaultValueFor(StorageConfigurationsConstants.LOADING_MODE_FLAG))
        .thenReturn(LOADING_MODE_DEFAULT.toString());
    when(defaultProvider.getDefaultValueFor(
            StorageConfigurationsConstants.MEMTABLE_FLUSH_THRESHOLD_BYTES_FLAG))
        .thenReturn(String.valueOf(MEMTABLE_FLUSH_BYTES_DEFAULT));
    when(defaultProvider.getDefaultValueFor(
            StorageConfigurationsConstants.SEGMENT_LEVEL_COMPACT_THRESHOLD_BYTES_FLAG))
        .thenReturn(String.valueOf(SEGMENT_LEVEL_FLUSH_BYTES_DEFAULT));
    when(defaultProvider.getDefaultValueFor(
            StorageConfigurationsConstants.COMPACTOR_EXEC_DELAY_MILLISECONDS_FLAG))
        .thenReturn(String.valueOf(COMPACTOR_DELAY_DEFAULT));
  }
}
