package dev.sbutler.bitflask.config.validators;

import com.beust.jcommander.ParameterException;
import dev.sbutler.bitflask.config.InvalidConfigurationException;

/** Validates that a string is non-blank according to {@link String#isBlank()}. */
public class NonBlankStringValidator implements Validator<String> {

  @Override
  public void validate(String name, String value) throws ParameterException {
    if (value.isBlank()) {
      throw new InvalidConfigurationException(
          "Parameter " + name + " should not be blank (found [" + value + "])");
    }
  }
}
