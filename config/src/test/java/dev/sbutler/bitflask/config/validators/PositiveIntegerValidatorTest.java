package dev.sbutler.bitflask.config.validators;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import dev.sbutler.bitflask.config.IllegalConfigurationException;
import org.junit.jupiter.api.Test;

public class PositiveIntegerValidatorTest {

  @Test
  void positiveValue() {
    // Arrange
    String flagName = "--flag";
    String value = "1";
    PositiveIntegerValidator validator = new PositiveIntegerValidator();
    // Act / Assert
    try {
      validator.validate(flagName, value);
    } catch (Exception e) {
      fail();
    }
  }

  @Test
  void zeroValue() {
    // Arrange
    String flagName = "--flag";
    String value = "0";
    PositiveIntegerValidator validator = new PositiveIntegerValidator();
    // Act
    IllegalConfigurationException exception =
        assertThrows(IllegalConfigurationException.class,
            () -> validator.validate(flagName, value));
    // Assert
    assertTrue(exception.getMessage().contains(flagName));
    assertTrue(exception.getMessage().contains(value));
  }

  @Test
  void negativeValue() {
    // Arrange
    String flagName = "--flag";
    String value = "-1";
    PositiveIntegerValidator validator = new PositiveIntegerValidator();
    // Act
    IllegalConfigurationException exception =
        assertThrows(IllegalConfigurationException.class,
            () -> validator.validate(flagName, value));
    // Assert
    assertTrue(exception.getMessage().contains(flagName));
    assertTrue(exception.getMessage().contains(value));
  }
}
