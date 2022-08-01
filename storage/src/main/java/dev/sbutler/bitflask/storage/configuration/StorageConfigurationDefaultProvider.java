package dev.sbutler.bitflask.storage.configuration;

import com.beust.jcommander.IDefaultProvider;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;

/**
 * Manages provided the default value for Storage configurations when a command line flag was not
 * passed.
 *
 * <p>If a ResourceBundle is provided, and it contains a value for a corresponding property, the
 * contained value will be provided. Otherwise, the hardcoded default value will be returned.
 */
public class StorageConfigurationDefaultProvider implements IDefaultProvider {

  static final String STORAGE_DISPATCHER_CAPACITY_PROPERTY_KEY = "storage.dispatcherCapacity";
  static final int DEFAULT_STORAGE_DISPATCHER_CAPACITY = 500;

  static final String STORAGE_STORE_DIRECTORY_PATH_PROPERTY_KEY = "storage.storeDirectory";
  static final Path DEFAULT_STORAGE_STORE_DIRECTORY_PATH = Paths.get(
      System.getProperty("user.home") + "/.bitflask/store/");

  static final String STORAGE_SEGMENT_SIZE_LIMIT_PROPERTY_KEY = "storage.segmentSizeLimit";
  static final long DEFAULT_STORAGE_SEGMENT_SIZE_LIMIT = 1048576L; // 1 MiB

  private final ResourceBundle resourceBundle;

  public StorageConfigurationDefaultProvider() {
    this.resourceBundle = null;
  }

  public StorageConfigurationDefaultProvider(ResourceBundle resourceBundle) {
    this.resourceBundle = resourceBundle;
  }

  @Override
  public String getDefaultValueFor(String optionName) {
    return switch (optionName) {
      case StorageConfiguration.STORAGE_DISPATCHER_CAPACITY_FLAG -> getStorageDispatcherCapacity();
      case StorageConfiguration.STORAGE_STORE_DIRECTORY_PATH_FLAG -> getStorageStoreDirectoryPath();
      case StorageConfiguration.STORAGE_SEGMENT_SIZE_LIMIT_FLAG -> getStorageSegmentSizeLimit();
      default -> null;
    };
  }

  private String getStorageDispatcherCapacity() {
    if (resourceBundle == null
        || !resourceBundle.containsKey(STORAGE_DISPATCHER_CAPACITY_PROPERTY_KEY)) {
      return String.valueOf(DEFAULT_STORAGE_DISPATCHER_CAPACITY);
    } else {
      return resourceBundle.getString(STORAGE_DISPATCHER_CAPACITY_PROPERTY_KEY);
    }
  }

  private String getStorageStoreDirectoryPath() {
    if (resourceBundle == null
        || !resourceBundle.containsKey(STORAGE_STORE_DIRECTORY_PATH_PROPERTY_KEY)) {
      return DEFAULT_STORAGE_STORE_DIRECTORY_PATH.toString();
    } else {
      return resourceBundle.getString(STORAGE_STORE_DIRECTORY_PATH_PROPERTY_KEY);
    }
  }

  private String getStorageSegmentSizeLimit() {
    if (resourceBundle == null
        || !resourceBundle.containsKey(STORAGE_SEGMENT_SIZE_LIMIT_PROPERTY_KEY)) {
      return String.valueOf(DEFAULT_STORAGE_SEGMENT_SIZE_LIMIT);
    } else {
      return resourceBundle.getString(STORAGE_SEGMENT_SIZE_LIMIT_PROPERTY_KEY);
    }
  }
}
