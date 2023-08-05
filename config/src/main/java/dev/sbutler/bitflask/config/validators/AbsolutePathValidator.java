package dev.sbutler.bitflask.config.validators;

import com.beust.jcommander.IParameterValidator;
import dev.sbutler.bitflask.config.InvalidConfigurationException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AbsolutePathValidator implements IParameterValidator {

  @Override
  public void validate(String name, String value) throws InvalidConfigurationException {
    Path providedPath = Paths.get(value);
    if (!providedPath.isAbsolute()) {
      throw new InvalidConfigurationException(
          "Parameter " + name + " should be an absolute path (found " + value + ")");
    }
  }
}
