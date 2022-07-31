package dev.sbutler.bitflask.storage.configuration;


import com.beust.jcommander.Parameter;
import java.util.ResourceBundle;

/**
 * Provides access to the storage engine's runtime configurations.
 *
 * <p>The configuration parameters can be set via command line flags or a property file. The
 * priority for defining the parameters:
 * <ol>
 *   <li>command line flags</li>
 *   <li>property file</li>
 *   <li>hardcoded value</li>
 * </ol>
 */
public class StorageConfiguration {

  @Parameter(names = "--storageDispatcherCapacity",
      description = "The maximum number of storage submissions that can be queued")
  private int storageDispatcherCapacity = 500;

  public StorageConfiguration() {
  }

  public StorageConfiguration(ResourceBundle resourceBundle) {
    if (resourceBundle.containsKey("storageDispatcherCapacity")) {
      storageDispatcherCapacity = Integer.parseInt(
          resourceBundle.getString("storageDispatcherCapacity"));
    }
  }

  public int getStorageDispatcherCapacity() {
    return storageDispatcherCapacity;
  }
}
