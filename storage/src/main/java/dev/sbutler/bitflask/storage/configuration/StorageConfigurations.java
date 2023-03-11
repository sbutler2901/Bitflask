package dev.sbutler.bitflask.storage.configuration;

import com.beust.jcommander.Parameter;
import dev.sbutler.bitflask.common.configuration.Configurations;
import dev.sbutler.bitflask.common.configuration.validators.AbsolutePathValidator;
import dev.sbutler.bitflask.common.configuration.validators.PositiveIntegerValidator;
import dev.sbutler.bitflask.common.configuration.validators.PositiveLongValidator;
import java.nio.file.Path;

/**
 * Provides access to the storage engine's runtime configurations.
 */
public class StorageConfigurations implements Configurations {

  @Parameter(names = StorageConfigurationsConstants.STORAGE_DISPATCHER_CAPACITY_FLAG,
      validateWith = PositiveIntegerValidator.class,
      description = "The maximum number of storage submissions that can be queued")
  private int storageDispatcherCapacity;

  @Parameter(names = StorageConfigurationsConstants.STORAGE_STORE_DIRECTORY_PATH_FLAG,
      validateWith = AbsolutePathValidator.class,
      description = "The directory path in which storage segments will be read & written. This must be an absolute path.")
  private Path storageStoreDirectoryPath;

  @Parameter(names = StorageConfigurationsConstants.STORAGE_SEGMENT_SIZE_LIMIT_FLAG,
      validateWith = PositiveLongValidator.class,
      description = "The size limit of a segment before a new one will be created")
  private long storageSegmentSizeLimit;

  @Parameter(names = StorageConfigurationsConstants.STORAGE_LOADING_MODE_FLAG,
      converter = StorageLoadingModeConverter.class,
      description = "The method used for Segment creation. 'create' will reuse pre-existing segments, loading their content at start. 'truncate' will overwrite pre-exiting segments ignoring their contents."
  )
  private StorageLoadingMode storageSegmentCreationMode;

  @Parameter(names = StorageConfigurationsConstants.STORAGE_COMPACTION_THRESHOLD_FLAG,
      validateWith = PositiveIntegerValidator.class,
      description = "The number of new segments to be created before compaction is performed"
  )
  private int storageCompactionThreshold;

  public int getStorageDispatcherCapacity() {
    return storageDispatcherCapacity;
  }

  public Path getStorageStoreDirectoryPath() {
    return storageStoreDirectoryPath;
  }

  public long getStorageSegmentSizeLimit() {
    return storageSegmentSizeLimit;
  }

  public StorageLoadingMode getStorageLoadingMode() {
    return storageSegmentCreationMode;
  }

  public int getStorageCompactionThreshold() {
    return storageCompactionThreshold;
  }

  @Override
  public String toString() {
    return "StorageConfigurations{" +
        "storageDispatcherCapacity=" + storageDispatcherCapacity +
        ", storageStoreDirectoryPath=" + storageStoreDirectoryPath +
        ", storageSegmentSizeLimit=" + storageSegmentSizeLimit +
        ", storageLoadingMode=" + storageSegmentCreationMode +
        ", storageCompactionThreshold=" + storageCompactionThreshold +
        '}';
  }
}
