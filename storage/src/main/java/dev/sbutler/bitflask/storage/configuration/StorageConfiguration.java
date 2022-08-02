package dev.sbutler.bitflask.storage.configuration;

import com.beust.jcommander.Parameter;
import dev.sbutler.bitflask.common.configuration.validators.AbsolutePathValidator;
import dev.sbutler.bitflask.common.configuration.validators.PositiveIntegerValidator;
import dev.sbutler.bitflask.common.configuration.validators.PositiveLongValidator;
import java.nio.file.Path;

/**
 * Provides access to the storage engine's runtime configurations.
 *
 * <p>It is required that this class is initialized using {@link com.beust.jcommander.JCommander}
 * accompanied by the default provider
 * {@link dev.sbutler.bitflask.common.configuration.ConfigurationDefaultProvider}.
 *
 * <p>The configuration parameters can be set via command line flags or a property file. The
 * priority order for defining the parameters is:
 * <ol>
 *   <li>command line flags</li>
 *   <li>property file</li>
 *   <li>hardcoded value</li>
 * </ol>
 *
 * <p>Note: an illegal parameter value will cause an
 * {@link dev.sbutler.bitflask.common.configuration.exceptions.IllegalConfigurationException} to be
 * thrown WITHOUT falling back to a lower priority parameter definition. For example, a negative
 * number provided for the storage dispatcher capacity via command line will throw an exception
 * rather than referring to the property file, or hardcoded value.
 */
public class StorageConfiguration {

  @Parameter(names = StorageConfigurationConstants.STORAGE_DISPATCHER_CAPACITY_FLAG,
      validateWith = PositiveIntegerValidator.class,
      description = "The maximum number of storage submissions that can be queued")
  private int storageDispatcherCapacity;

  @Parameter(names = StorageConfigurationConstants.STORAGE_STORE_DIRECTORY_PATH_FLAG,
      validateWith = AbsolutePathValidator.class,
      description = "The directory path in which storage segments will be read & written. This must be an absolute path.")
  private Path storageStoreDirectoryPath;

  @Parameter(names = StorageConfigurationConstants.STORAGE_SEGMENT_SIZE_LIMIT_FLAG,
      validateWith = PositiveLongValidator.class,
      description = "The size limit of a segment before a new one will be created")
  private long storageSegmentSizeLimit;

  public int getStorageDispatcherCapacity() {
    return storageDispatcherCapacity;
  }

  public Path getStorageStoreDirectoryPath() {
    return storageStoreDirectoryPath;
  }

  public long getStorageSegmentSizeLimit() {
    return storageSegmentSizeLimit;
  }
}
