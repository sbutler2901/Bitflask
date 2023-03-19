package dev.sbutler.bitflask.storage.configuration;

import com.beust.jcommander.Parameter;
import dev.sbutler.bitflask.common.configuration.Configurations;
import dev.sbutler.bitflask.common.configuration.validators.AbsolutePathValidator;
import dev.sbutler.bitflask.common.configuration.validators.PositiveIntegerValidator;
import java.nio.file.Path;

/**
 * Provides access to the storage engine's runtime configurations.
 */
public class StorageConfigurations implements Configurations {

  @Parameter(names = StorageConfigurationsConstants.DISPATCHER_CAPACITY_FLAG,
      validateWith = PositiveIntegerValidator.class,
      description = "The maximum number of storage submissions that can be queued")
  private int dispatcherCapacity;

  @Parameter(names = StorageConfigurationsConstants.STORE_DIRECTORY_PATH_FLAG,
      validateWith = AbsolutePathValidator.class,
      description = "The directory path in which storage segments will be read & written."
          + " This must be an absolute path.")
  private Path storeDirectoryPath;

  @Parameter(names = StorageConfigurationsConstants.LOADING_MODE_FLAG,
      converter = StorageLoadingModeConverter.class,
      description = "The method used for Segment creation. 'create' will reuse pre-existing" +
          " segments, loading their content at start. 'truncate' will overwrite pre-exiting" +
          " segments ignoring their contents.")
  private StorageLoadingMode loadingMode;

  @Parameter(names = StorageConfigurationsConstants.MEMTABLE_FLUSH_THRESHOLD_MB_FLAG,
      validateWith = PositiveIntegerValidator.class,
      description = "The number of MBs the Memtable must exceed before being flushed to disk")
  private int memtableFlushThresholdMB;

  @Parameter(names = StorageConfigurationsConstants.SEGMENT_LEVEL_COMPACT_THRESHOLD_MB_FLAG,
      validateWith = PositiveIntegerValidator.class,
      description = "The number of MBs a Segment Level must exceed before being compacted")
  private int segmentLevelFlushThresholdMB;

  public int getDispatcherCapacity() {
    return dispatcherCapacity;
  }

  public Path getStoreDirectoryPath() {
    return storeDirectoryPath;
  }

  public StorageLoadingMode getStorageLoadingMode() {
    return loadingMode;
  }

  public int getMemtableFlushThresholdMB() {
    return memtableFlushThresholdMB;
  }

  public int getSegmentLevelFlushThresholdMB() {
    return segmentLevelFlushThresholdMB;
  }

  @Override
  public String toString() {
    return "StorageConfigurations{" +
        "storageDispatcherCapacity=" + dispatcherCapacity +
        ", storageStoreDirectoryPath=" + storeDirectoryPath +
        ", storageLoadingMode=" + loadingMode +
        ", memtableFlushThresholdMB=" + memtableFlushThresholdMB +
        ", segmentLevelFlushThresholdMB=" + segmentLevelFlushThresholdMB +
        '}';
  }
}
