package dev.sbutler.bitflask.config.validators;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;
import dev.sbutler.bitflask.config.InvalidConfigurationException;

public class PositiveLongValidator implements IParameterValidator {

  @Override
  public void validate(String name, String value) throws ParameterException {
    long n = Long.parseLong(value);
    if (Long.signum(n) < 1) {
      throw new InvalidConfigurationException(
          "Parameter " + name + " should be positive (found " + value + ")");
    }
  }
}
