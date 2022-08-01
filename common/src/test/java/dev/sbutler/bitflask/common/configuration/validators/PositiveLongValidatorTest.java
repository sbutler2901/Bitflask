package dev.sbutler.bitflask.common.configuration.validators;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import dev.sbutler.bitflask.common.configuration.exceptions.IllegalConfigurationException;
import org.junit.jupiter.api.Test;

public class PositiveLongValidatorTest {

  @Test
  void positiveValue() {
    // Arrange
    String flagName = "--flag";
    String value = "1";
    PositiveLongValidator validator = new PositiveLongValidator();
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
    PositiveLongValidator validator = new PositiveLongValidator();
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
    PositiveLongValidator validator = new PositiveLongValidator();
    // Act
    IllegalConfigurationException exception =
        assertThrows(IllegalConfigurationException.class,
            () -> validator.validate(flagName, value));
    // Assert
    assertTrue(exception.getMessage().contains(flagName));
    assertTrue(exception.getMessage().contains(value));

  }
}
