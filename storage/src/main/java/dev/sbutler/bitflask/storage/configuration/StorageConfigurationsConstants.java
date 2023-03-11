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

  // Storage Segment Size Limit
  static final String STORAGE_SEGMENT_SIZE_LIMIT_FLAG = "--storageSegmentSizeLimit";
  static final String STORAGE_SEGMENT_SIZE_LIMIT_PROPERTY_KEY = "storage.segmentSizeLimit";
  static final long STORAGE_SEGMENT_SIZE_LIMIT_DEFAULT = 1048576L; // 1 MiB
  static final Configuration STORAGE_SEGMENT_SIZE_LIMIT_CONFIGURATION = new Configuration(
      ImmutableList.of(STORAGE_SEGMENT_SIZE_LIMIT_FLAG),
      STORAGE_SEGMENT_SIZE_LIMIT_PROPERTY_KEY,
      STORAGE_SEGMENT_SIZE_LIMIT_DEFAULT);

  // Storage Loading Mode
  static final String STORAGE_LOADING_MODE_FLAG = "--storageSegmentCreationMode";
  static final String STORAGE_LOADING_MODE_PROPERTY_KEY = "storage.loadingMode";
  static final StorageLoadingMode STORAGE_LOADING_MODE_DEFAULT = StorageLoadingMode.LOAD;
  static final Configuration STORAGE_LOADING_MODE_CONFIGURATION = new Configuration(
      ImmutableList.of(STORAGE_LOADING_MODE_FLAG),
      STORAGE_LOADING_MODE_PROPERTY_KEY,
      STORAGE_LOADING_MODE_DEFAULT);

  // Storage Compaction Threshold
  static final String STORAGE_COMPACTION_THRESHOLD_FLAG = "--storageCompactionThreshold";
  static final String STORAGE_COMPACTION_THRESHOLD_PROPERTY_KEY = "storage.compactionThreshold";
  static final int STORAGE_COMPACTION_THRESHOLD_DEFAULT = 3;
  static final Configuration STORAGE_COMPACTION_THRESHOLD_CONFIGURATION = new Configuration(
      ImmutableList.of(STORAGE_COMPACTION_THRESHOLD_FLAG),
      STORAGE_COMPACTION_THRESHOLD_PROPERTY_KEY,
      STORAGE_COMPACTION_THRESHOLD_DEFAULT);

  public static final ConfigurationFlagMap STORAGE_FLAG_TO_CONFIGURATION_MAP =
      new ConfigurationFlagMap.Builder()
          .put(STORAGE_DISPATCHER_CAPACITY_FLAG, STORAGE_DISPATCHER_CONFIGURATION)
          .put(STORAGE_STORE_DIRECTORY_PATH_FLAG, STORAGE_STORE_DIRECTORY_PATH_CONFIGURATION)
          .put(STORAGE_SEGMENT_SIZE_LIMIT_FLAG, STORAGE_SEGMENT_SIZE_LIMIT_CONFIGURATION)
          .put(STORAGE_LOADING_MODE_FLAG, STORAGE_LOADING_MODE_CONFIGURATION)
          .put(STORAGE_COMPACTION_THRESHOLD_FLAG, STORAGE_COMPACTION_THRESHOLD_CONFIGURATION)
          .build();

  private StorageConfigurationsConstants() {
  }
}
