package dev.sbutler.bitflask.config.validators;

import dev.sbutler.bitflask.config.InvalidConfigurationException;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Validates that a path argument is an absolute path according to {@link Path#isAbsolute()}. */
public class AbsolutePathValidator implements Validator<String> {

  @Override
  public void validate(String name, String value) {
    Path providedPath = Paths.get(value);
    if (!providedPath.isAbsolute()) {
      throw new InvalidConfigurationException(
          "Parameter " + name + " should be an absolute path (found " + value + ")");
    }
  }
}
