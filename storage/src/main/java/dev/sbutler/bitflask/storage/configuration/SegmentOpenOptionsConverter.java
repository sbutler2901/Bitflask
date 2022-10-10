package dev.sbutler.bitflask.storage.configuration;

import com.beust.jcommander.IStringConverter;
import dev.sbutler.bitflask.common.configuration.exceptions.IllegalConfigurationException;
import java.nio.file.StandardOpenOption;

class SegmentOpenOptionsConverter implements IStringConverter<StandardOpenOption> {

  static final String CREATE_ARG = "create";
  static final String TRUNCATE_ARG = "truncate";

  private static final String ERROR_MESSAGE = "Parameter %s should be positive (found %s)";

  @Override
  public StandardOpenOption convert(String value) {
    String normalizedValue = value.trim().toLowerCase();
    if (normalizedValue.equals(CREATE_ARG)) {
      return StandardOpenOption.CREATE;
    } else if (normalizedValue.equals(TRUNCATE_ARG)) {
      return StandardOpenOption.TRUNCATE_EXISTING;
    } else {
      throw new IllegalConfigurationException(
          String.format(ERROR_MESSAGE,
              StorageConfigurationConstants.STORAGE_SEGMENT_CREATION_MODE_FLAG, value));
    }
  }
}
