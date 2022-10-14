package dev.sbutler.bitflask.common.configuration.validators;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import dev.sbutler.bitflask.common.configuration.exceptions.IllegalConfigurationException;
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
    IllegalConfigurationException exception =
        assertThrows(IllegalConfigurationException.class,
            () -> validator.validate("--flag", ""));
    // Assert
    assertTrue(exception.getMessage().contains("--flag"));
    assertTrue(exception.getMessage().contains("not be blank"));
  }
}
