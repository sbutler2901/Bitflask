package dev.sbutler.bitflask.config.validators;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;
import dev.sbutler.bitflask.config.InvalidConfigurationException;

public class NonBlankStringValidator implements IParameterValidator {

  @Override
  public void validate(String name, String value) throws ParameterException {
    if (value.isBlank()) {
      throw new InvalidConfigurationException(
          "Parameter " + name + " should not be blank (found [" + value + "])");
    }
  }
}
