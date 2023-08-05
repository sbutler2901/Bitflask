package dev.sbutler.bitflask.config.validators;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import dev.sbutler.bitflask.config.InvalidConfigurationException;
import org.junit.jupiter.api.Test;

public class PositiveIntegerValidatorTest {

  @Test
  void positiveValue() {
    PositiveIntegerValidator validator = new PositiveIntegerValidator();

    try {
      validator.validate("intArg", 1);
    } catch (Exception e) {
      fail();
    }
  }

  @Test
  void zeroValue() {
    String name = "intArg";
    int value = 0;
    PositiveIntegerValidator validator = new PositiveIntegerValidator();

    InvalidConfigurationException exception =
        assertThrows(InvalidConfigurationException.class, () -> validator.validate(name, value));

    assertTrue(exception.getMessage().contains(name));
    assertTrue(exception.getMessage().contains(Integer.toString(value)));
  }

  @Test
  void negativeValue() {
    String name = "intArg";
    int value = -1;
    PositiveIntegerValidator validator = new PositiveIntegerValidator();

    InvalidConfigurationException exception =
        assertThrows(InvalidConfigurationException.class, () -> validator.validate(name, value));

    assertTrue(exception.getMessage().contains(name));
    assertTrue(exception.getMessage().contains(Integer.toString(value)));
  }
}
