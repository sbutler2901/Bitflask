package dev.sbutler.bitflask.config.validators;

/** General interface for config argument validators. */
public interface Validator<T> {

  /**
   * Validates the supported argument throwing an {@link
   * dev.sbutler.bitflask.config.InvalidConfigurationException} if invalid.
   */
  void validate(String name, T value);
}
