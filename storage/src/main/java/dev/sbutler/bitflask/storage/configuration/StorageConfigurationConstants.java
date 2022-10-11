package dev.sbutler.bitflask.storage.configuration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import dev.sbutler.bitflask.common.configuration.Configuration;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class StorageConfigurationConstants {

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

  // Storage Segment Creation Mode
  enum StorageSegmentCreationModeArgs {
    CREATE("create"),
    TRUNCATE("truncate");

    private final String rawArg;

    StorageSegmentCreationModeArgs(String rawArg) {
      this.rawArg = rawArg;
    }

    String getRawArg() {
      return rawArg;
    }
  }

  static final String STORAGE_SEGMENT_CREATION_MODE_NAME = "StorageSegmentCreationMode";
  static final String STORAGE_SEGMENT_CREATION_MODE_FLAG = "--storageSegmentCreationMode";
  static final String STORAGE_SEGMENT_CREATION_MODE_PROPERTY_KEY = "storage.segmentCreationMode";
  static final StandardOpenOption STORAGE_SEGMENT_CREATION_MODE_DEFAULT = StandardOpenOption.CREATE;
  static final Configuration STORAGE_SEGMENT_CREATION_MODE_CONFIGURATION = new Configuration(
      ImmutableList.of(STORAGE_SEGMENT_CREATION_MODE_FLAG),
      STORAGE_SEGMENT_CREATION_MODE_PROPERTY_KEY,
      STORAGE_SEGMENT_CREATION_MODE_DEFAULT);

  // Storage Compaction Threshold
  static final String STORAGE_COMPACTION_THRESHOLD_FLAG = "--storageCompactionThreshold";
  static final String STORAGE_COMPACTION_THRESHOLD_PROPERTY_KEY = "storage.compactionThreshold";
  static final int STORAGE_COMPACTION_THRESHOLD_DEFAULT = 3;
  static final Configuration STORAGE_COMPACTION_THRESHOLD_CONFIGURATION = new Configuration(
      ImmutableList.of(STORAGE_COMPACTION_THRESHOLD_FLAG),
      STORAGE_COMPACTION_THRESHOLD_PROPERTY_KEY,
      STORAGE_COMPACTION_THRESHOLD_DEFAULT);

  public static final ImmutableMap<String, Configuration> STORAGE_FLAG_TO_CONFIGURATION_MAP =
      new ImmutableMap.Builder<String, Configuration>()
          .put(STORAGE_DISPATCHER_CAPACITY_FLAG, STORAGE_DISPATCHER_CONFIGURATION)
          .put(STORAGE_STORE_DIRECTORY_PATH_FLAG, STORAGE_STORE_DIRECTORY_PATH_CONFIGURATION)
          .put(STORAGE_SEGMENT_SIZE_LIMIT_FLAG, STORAGE_SEGMENT_SIZE_LIMIT_CONFIGURATION)
          .put(STORAGE_SEGMENT_CREATION_MODE_FLAG, STORAGE_SEGMENT_CREATION_MODE_CONFIGURATION)
          .put(STORAGE_COMPACTION_THRESHOLD_FLAG, STORAGE_COMPACTION_THRESHOLD_CONFIGURATION)
          .build();

  private StorageConfigurationConstants() {
  }
}
