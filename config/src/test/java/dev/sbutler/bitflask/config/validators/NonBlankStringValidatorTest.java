package dev.sbutler.bitflask.config.validators;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import dev.sbutler.bitflask.config.InvalidConfigurationException;
import org.junit.jupiter.api.Test;

public class NonBlankStringValidatorTest {

  @Test
  public void nonBlankValue() {
    // Arrange
    NonBlankStringValidator validator = new NonBlankStringValidator();
    // Act / Assert
    try {
      validator.validate("--flag", "localhost");
    } catch (Exception e) {
      fail();
    }
  }

  @Test
  public void blankValue() {
    // Arrange
    NonBlankStringValidator validator = new NonBlankStringValidator();
    // Act
    InvalidConfigurationException exception =
        assertThrows(InvalidConfigurationException.class, () -> validator.validate("--flag", ""));
    // Assert
    assertTrue(exception.getMessage().contains("--flag"));
    assertTrue(exception.getMessage().contains("not be blank"));
  }
}
