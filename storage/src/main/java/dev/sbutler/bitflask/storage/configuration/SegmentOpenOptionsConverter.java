package dev.sbutler.bitflask.storage.configuration;

import com.beust.jcommander.IStringConverter;
import dev.sbutler.bitflask.common.configuration.exceptions.IllegalConfigurationException;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurationsConstants.StorageSegmentCreationModeArgs;
import java.nio.file.StandardOpenOption;

/**
 * Handles converting the StorageSegmentCreationMode configuration string provided by the user into
 * {@link java.nio.file.StandardOpenOption}.
 *
 * <p>An unsupported option will cause an {@link IllegalConfigurationException} to be thrown.
 */
class SegmentOpenOptionsConverter implements IStringConverter<StandardOpenOption> {

  @Override
  public StandardOpenOption convert(String value) {
    String normalizedValue = value.trim().toUpperCase();
    StorageSegmentCreationModeArgs mode;
    try {
      mode = StorageSegmentCreationModeArgs.valueOf(normalizedValue);
    } catch (IllegalArgumentException e) {
      throw new IllegalConfigurationException(
          String.format("Parameter %s was invalid (found %s)",
              StorageConfigurationsConstants.STORAGE_SEGMENT_CREATION_MODE_NAME, value));
    }
    if (mode.equals(StorageSegmentCreationModeArgs.CREATE)) {
      return StandardOpenOption.CREATE;
    } else {
      return StandardOpenOption.TRUNCATE_EXISTING;
    }
  }
}
