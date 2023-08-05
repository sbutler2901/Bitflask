package dev.sbutler.bitflask.config.validators;

import com.beust.jcommander.IParameterValidator;
import dev.sbutler.bitflask.config.IllegalConfigurationException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AbsolutePathValidator implements IParameterValidator {

  @Override
  public void validate(String name, String value) throws IllegalConfigurationException {
    Path providedPath = Paths.get(value);
    if (!providedPath.isAbsolute()) {
      throw new IllegalConfigurationException(
          "Parameter " + name + " should be an absolute path (found " + value + ")");
    }
  }
}
