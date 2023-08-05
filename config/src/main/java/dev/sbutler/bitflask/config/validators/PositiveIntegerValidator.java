package dev.sbutler.bitflask.config.validators;

import com.beust.jcommander.IParameterValidator;
import dev.sbutler.bitflask.config.InvalidConfigurationException;

public class PositiveIntegerValidator implements IParameterValidator {

  public void validate(String name, String value) throws InvalidConfigurationException {
    int n = Integer.parseInt(value);
    if (Integer.signum(n) < 1) {
      throw new InvalidConfigurationException(
          "Parameter " + name + " should be positive (found " + value + ")");
    }
  }
}
