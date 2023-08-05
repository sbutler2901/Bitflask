package dev.sbutler.bitflask.config.validators;

import dev.sbutler.bitflask.config.InvalidConfigurationException;

/** Validates that an integer is positive according to {@link Integer#signum(int)}. */
public class PositiveIntegerValidator implements Validator<Integer> {

  @Override
  public void validate(String name, Integer value) {
    if (Integer.signum(value) < 1) {
      throw new InvalidConfigurationException(
          "Parameter " + name + " should be positive (found " + value + ")");
    }
  }
}
