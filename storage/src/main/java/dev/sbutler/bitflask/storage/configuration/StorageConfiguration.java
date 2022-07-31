package dev.sbutler.bitflask.storage.configuration;


import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.flogger.FluentLogger;
import java.nio.file.Path;
import java.nio.file.Paths;
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

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  @Parameter(names = "--storageDispatcherCapacity",
      description = "The maximum number of storage submissions that can be queued")
  private int storageDispatcherCapacity = 500;

  @Parameter(names = "--storageStoreDirectory",
      validateWith = AbsolutePathValidator.class,
      description = "The directory path in which storage segments will be read & written. This must be an absolute path.")
  private Path segmentDirPath = Paths.get(System.getProperty("user.home") + "/.bitflask/store/");

  public StorageConfiguration() {
  }

  public StorageConfiguration(ResourceBundle resourceBundle) {
    if (resourceBundle.containsKey("storage.dispatcherCapacity")) {
      storageDispatcherCapacity = Integer.parseInt(
          resourceBundle.getString("storage.dispatcherCapacity"));
    }
    if (resourceBundle.containsKey("storage.storeDirectory")) {
      Path pathFromProperties = Paths.get(resourceBundle.getString("storage.storeDirectory"));
      if (pathFromProperties.isAbsolute()) {
        segmentDirPath = pathFromProperties;
      } else {
        logger.atWarning().log(
            "The storage.storeDirectory path provided in configuration properties was not absolute. Ignoring.");
      }
    }
  }

  public int getStorageDispatcherCapacity() {
    return storageDispatcherCapacity;
  }

  public Path getSegmentDirPath() {
    return segmentDirPath;
  }

  public static class AbsolutePathValidator implements IParameterValidator {

    @Override
    public void validate(String name, String value) throws ParameterException {
      Path providedPath = Paths.get(value);
      if (!providedPath.isAbsolute()) {
        throw new ParameterException(
            "Parameter " + name + " should be an absolute path (found " + value + ")");
      }
    }
  }

}
