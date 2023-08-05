package dev.sbutler.bitflask.config.validators;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import dev.sbutler.bitflask.config.InvalidConfigurationException;
import org.junit.jupiter.api.Test;

public class AbsolutePathValidatorTest {

  @Test
  void absolutePath() {
    // Arrange
    String flagName = "--flag";
    String value = "/tmp/test";
    AbsolutePathValidator validator = new AbsolutePathValidator();
    // Act / Assert
    try {
      validator.validate(flagName, value);
    } catch (Exception e) {
      fail();
    }
  }

  @Test
  void relativePath() {
    // Arrange
    String flagName = "--flag";
    String value = "~/test";
    AbsolutePathValidator validator = new AbsolutePathValidator();
    // Act
    InvalidConfigurationException exception =
        assertThrows(
            InvalidConfigurationException.class, () -> validator.validate(flagName, value));
    // Assert
    assertTrue(exception.getMessage().contains(flagName));
    assertTrue(exception.getMessage().contains(value));
  }

}
