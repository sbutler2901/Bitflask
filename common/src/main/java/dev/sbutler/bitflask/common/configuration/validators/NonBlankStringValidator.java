package dev.sbutler.bitflask.common.configuration.validators;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;
import dev.sbutler.bitflask.common.configuration.exceptions.IllegalConfigurationException;

public class NonBlankStringValidator implements IParameterValidator {

  @Override
  public void validate(String name, String value) throws ParameterException {
    if (value.isBlank()) {
      throw new IllegalConfigurationException("Parameter " + name
          + " should not be blank (found [" + value + "])");
    }
  }
}
