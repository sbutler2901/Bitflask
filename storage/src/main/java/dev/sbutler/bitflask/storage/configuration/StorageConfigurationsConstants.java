package dev.sbutler.bitflask.storage.configuration;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.common.configuration.Configuration;
import dev.sbutler.bitflask.common.configuration.ConfigurationFlagMap;
import java.nio.file.Path;
import java.nio.file.Paths;

public class StorageConfigurationsConstants {

  // Storage Dispatcher Capacity
  static final String STORAGE_DISPATCHER_CAPACITY_FLAG = "--storageDispatcherCapacity";
  static final String STORAGE_DISPATCHER_CAPACITY_PROPERTY_KEY = "storage.dispatcherCapacity";
  static final int STORAGE_DISPATCHER_CAPACITY_DEFAULT = 500;
  static final Configuration STORAGE_DISPATCHER_CONFIGURATION = new Configuration(
      ImmutableList.of(STORAGE_DISPATCHER_CAPACITY_FLAG),
      STORAGE_DISPATCHER_CAPACITY_PROPERTY_KEY,
      STORAGE_DISPATCHER_CAPACITY_DEFAULT);

  // Storage Store Directory Path
  static final String STORAGE_STORE_DIRECTORY_PATH_FLAG = "--storageStoreDirectoryPath";
  static final String STORAGE_STORE_DIRECTORY_PATH_PROPERTY_KEY = "storage.storeDirectory";
  static final Path STORAGE_STORE_DIRECTORY_PATH_DEFAULT = Paths.get(
      System.getProperty("user.home") + "/.bitflask/store/");
  static final Configuration STORAGE_STORE_DIRECTORY_PATH_CONFIGURATION = new Configuration(
      ImmutableList.of(STORAGE_STORE_DIRECTORY_PATH_FLAG),
      STORAGE_STORE_DIRECTORY_PATH_PROPERTY_KEY,
      STORAGE_STORE_DIRECTORY_PATH_DEFAULT);

  // Storage Loading Mode
  static final String STORAGE_LOADING_MODE_FLAG = "--storageSegmentCreationMode";
  static final String STORAGE_LOADING_MODE_PROPERTY_KEY = "storage.loadingMode";
  static final StorageLoadingMode STORAGE_LOADING_MODE_DEFAULT = StorageLoadingMode.LOAD;
  static final Configuration STORAGE_LOADING_MODE_CONFIGURATION = new Configuration(
      ImmutableList.of(STORAGE_LOADING_MODE_FLAG),
      STORAGE_LOADING_MODE_PROPERTY_KEY,
      STORAGE_LOADING_MODE_DEFAULT);

  // Storage Memtable Flush Size
  static final String STORAGE_MEMTABLE_FLUSH_THRESHOLD_MB_FLAG = "--storageMemtableFlushThresholdMB";
  static final String STORAGE_MEMTABLE_FLUSH_THRESHOLD_MB_PROPERTY_KEY = "storage.memtableFlushThresholdMB";
  static final int STORAGE_MEMTABLE_FLUSH_THRESHOLD_MB_DEFAULT = 1;
  static final Configuration STORAGE_MEMTABLE_FLUSH_THRESHOLD_MB_CONFIGURATION = new Configuration(
      ImmutableList.of(STORAGE_MEMTABLE_FLUSH_THRESHOLD_MB_FLAG),
      STORAGE_MEMTABLE_FLUSH_THRESHOLD_MB_PROPERTY_KEY,
      STORAGE_MEMTABLE_FLUSH_THRESHOLD_MB_DEFAULT);

  // Storage Segment Level Flush Size
  static final String STORAGE_SEGMENT_LEVEL_COMPACT_THRESHOLD_MB_FLAG = "--storageMemtableCompactThresholdMB";
  static final String STORAGE_SEGMENT_LEVEL_COMPACT_THRESHOLD_MB_PROPERTY_KEY = "storage.memtableCompactThresholdMB";
  static final int STORAGE_SEGMENT_LEVEL_COMPACT_THRESHOLD_MB_DEFAULT = 5;
  static final Configuration STORAGE_SEGMENT_LEVEL_COMPACT_THRESHOLD_MB_CONFIGURATION = new Configuration(
      ImmutableList.of(STORAGE_SEGMENT_LEVEL_COMPACT_THRESHOLD_MB_FLAG),
      STORAGE_SEGMENT_LEVEL_COMPACT_THRESHOLD_MB_PROPERTY_KEY,
      STORAGE_SEGMENT_LEVEL_COMPACT_THRESHOLD_MB_DEFAULT);

  public static final ConfigurationFlagMap STORAGE_FLAG_TO_CONFIGURATION_MAP =
      new ConfigurationFlagMap.Builder()
          .put(STORAGE_DISPATCHER_CAPACITY_FLAG, STORAGE_DISPATCHER_CONFIGURATION)
          .put(STORAGE_STORE_DIRECTORY_PATH_FLAG, STORAGE_STORE_DIRECTORY_PATH_CONFIGURATION)
          .put(STORAGE_LOADING_MODE_FLAG, STORAGE_LOADING_MODE_CONFIGURATION)
          .put(STORAGE_MEMTABLE_FLUSH_THRESHOLD_MB_FLAG,
              STORAGE_MEMTABLE_FLUSH_THRESHOLD_MB_CONFIGURATION)
          .put(STORAGE_SEGMENT_LEVEL_COMPACT_THRESHOLD_MB_FLAG,
              STORAGE_SEGMENT_LEVEL_COMPACT_THRESHOLD_MB_CONFIGURATION)
          .build();

  private StorageConfigurationsConstants() {
  }
}
