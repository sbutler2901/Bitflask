package dev.sbutler.bitflask.storage.configuration;

import com.beust.jcommander.IStringConverter;
import dev.sbutler.bitflask.common.configuration.exceptions.IllegalConfigurationException;

/**
 * Handles converting user input into a {@link StorageLoadingMode}.
 */
final class StorageLoadingModeConverter implements IStringConverter<StorageLoadingMode> {

  @Override
  public StorageLoadingMode convert(String value) {
    try {
      return StorageLoadingMode.valueOf(StorageLoadingMode.normalizeValue(value));
    } catch (IllegalArgumentException e) {
      throw new IllegalConfigurationException(
          String.format("Parameter %s was invalid (found %s)",
              StorageConfigurationsConstants.STORAGE_LOADING_MODE_FLAG, value));
    }
  }
}
