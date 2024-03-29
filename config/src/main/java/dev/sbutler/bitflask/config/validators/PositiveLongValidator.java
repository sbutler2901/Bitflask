package dev.sbutler.bitflask.config.validators;

import dev.sbutler.bitflask.config.InvalidConfigurationException;

/** Validates that a long is positive according to {@link Long#signum(long)}. */
public class PositiveLongValidator implements Validator<Long> {

  @Override
  public void validate(String name, Long value) {
    if (Long.signum(value) < 1) {
      throw new InvalidConfigurationException(
          "Parameter " + name + " should be positive (found " + value + ")");
    }
  }
}
