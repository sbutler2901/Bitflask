package dev.sbutler.bitflask.storage.configuration;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.common.configuration.Configuration;
import dev.sbutler.bitflask.common.configuration.ConfigurationFlagMap;
import java.nio.file.Path;
import java.nio.file.Paths;

public class StorageConfigurationsConstants {

  // Store Directory Path
  static final String STORE_DIRECTORY_PATH_FLAG = "--storageStoreDirectoryPath";
  static final String STORE_DIRECTORY_PATH_PROPERTY_KEY = "storage.storeDirectory";
  static final Path STORE_DIRECTORY_PATH_DEFAULT = Paths.get(
      System.getProperty("user.home") + "/.bitflask/store/");
  static final Configuration STORAGE_STORE_DIRECTORY_PATH_CONFIGURATION = new Configuration(
      ImmutableList.of(STORE_DIRECTORY_PATH_FLAG),
      STORE_DIRECTORY_PATH_PROPERTY_KEY,
      STORE_DIRECTORY_PATH_DEFAULT);

  // Loading Mode
  static final String LOADING_MODE_FLAG = "--storageSegmentCreationMode";
  static final String LOADING_MODE_PROPERTY_KEY = "storage.loadingMode";
  static final StorageLoadingMode LOADING_MODE_DEFAULT = StorageLoadingMode.LOAD;
  static final Configuration LOADING_MODE_CONFIGURATION = new Configuration(
      ImmutableList.of(LOADING_MODE_FLAG),
      LOADING_MODE_PROPERTY_KEY,
      LOADING_MODE_DEFAULT);

  // Memtable Flush Size
  static final String MEMTABLE_FLUSH_THRESHOLD_BYTES_FLAG = "--storageMemtableFlushThresholdBytes";
  static final String MEMTABLE_FLUSH_THRESHOLD_BYTES_PROPERTY_KEY = "storage.memtableFlushThresholdBytes";
  static final long MEMTABLE_FLUSH_THRESHOLD_BYTES_DEFAULT = 1048576L; // 1 MiB
  static final Configuration MEMTABLE_FLUSH_THRESHOLD_BYTES_CONFIGURATION = new Configuration(
      ImmutableList.of(MEMTABLE_FLUSH_THRESHOLD_BYTES_FLAG),
      MEMTABLE_FLUSH_THRESHOLD_BYTES_PROPERTY_KEY,
      MEMTABLE_FLUSH_THRESHOLD_BYTES_DEFAULT);

  // Segment Level Flush Size
  static final String SEGMENT_LEVEL_COMPACT_THRESHOLD_BYTES_FLAG = "--storageSegmentLevelCompactThresholdBytes";
  static final String SEGMENT_LEVEL_COMPACT_THRESHOLD_BYTES_PROPERTY_KEY = "storage.segmentLevelCompactThresholdBytes";
  static final long SEGMENT_LEVEL_COMPACT_THRESHOLD_BYTES_DEFAULT = 5242880L; // 5 MiB
  static final Configuration SEGMENT_LEVEL_COMPACT_THRESHOLD_BYTES_CONFIGURATION = new Configuration(
      ImmutableList.of(SEGMENT_LEVEL_COMPACT_THRESHOLD_BYTES_FLAG),
      SEGMENT_LEVEL_COMPACT_THRESHOLD_BYTES_PROPERTY_KEY,
      SEGMENT_LEVEL_COMPACT_THRESHOLD_BYTES_DEFAULT);

  // Compactor Execution Frequency
  static final String COMPACTOR_EXEC_DELAY_MILLISECONDS_FLAG = "--storageCompactorExecDelayMilliSeconds";
  static final String COMPACTOR_EXEC_DELAY_MILLISECONDS_PROPERTY_KEY = "storage.compactorExecDelayMilliSeconds";
  static final long COMPACTOR_EXEC_DELAY_MILLISECONDS_DEFAULT = 5000; // 5 second
  static final Configuration COMPACTOR_EXEC_DELAY_MILLISECONDS_CONFIGURATION = new Configuration(
      ImmutableList.of(COMPACTOR_EXEC_DELAY_MILLISECONDS_FLAG),
      COMPACTOR_EXEC_DELAY_MILLISECONDS_PROPERTY_KEY,
      COMPACTOR_EXEC_DELAY_MILLISECONDS_DEFAULT);

  public static final ConfigurationFlagMap STORAGE_FLAG_TO_CONFIGURATION_MAP =
      new ConfigurationFlagMap.Builder()
          .put(STORE_DIRECTORY_PATH_FLAG, STORAGE_STORE_DIRECTORY_PATH_CONFIGURATION)
          .put(LOADING_MODE_FLAG, LOADING_MODE_CONFIGURATION)
          .put(MEMTABLE_FLUSH_THRESHOLD_BYTES_FLAG, MEMTABLE_FLUSH_THRESHOLD_BYTES_CONFIGURATION)
          .put(SEGMENT_LEVEL_COMPACT_THRESHOLD_BYTES_FLAG,
              SEGMENT_LEVEL_COMPACT_THRESHOLD_BYTES_CONFIGURATION)
          .put(COMPACTOR_EXEC_DELAY_MILLISECONDS_FLAG,
              COMPACTOR_EXEC_DELAY_MILLISECONDS_CONFIGURATION)
          .build();

  private StorageConfigurationsConstants() {
  }
}
