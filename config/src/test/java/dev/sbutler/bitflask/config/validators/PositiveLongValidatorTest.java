package dev.sbutler.bitflask.config.validators;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import dev.sbutler.bitflask.config.InvalidConfigurationException;
import org.junit.jupiter.api.Test;

public class PositiveLongValidatorTest {

  @Test
  void positiveValue() {
    PositiveLongValidator validator = new PositiveLongValidator();

    try {
      validator.validate("longArg", 1L);
    } catch (Exception e) {
      fail();
    }
  }

  @Test
  void zeroValue() {
    String name = "longArg";
    long value = 0L;
    PositiveLongValidator validator = new PositiveLongValidator();

    InvalidConfigurationException exception =
        assertThrows(InvalidConfigurationException.class, () -> validator.validate(name, value));

    assertTrue(exception.getMessage().contains(name));
    assertTrue(exception.getMessage().contains(Long.toString(value)));
  }

  @Test
  void negativeValue() {
    String name = "longArg";
    long value = -1L;
    PositiveLongValidator validator = new PositiveLongValidator();

    InvalidConfigurationException exception =
        assertThrows(InvalidConfigurationException.class, () -> validator.validate(name, value));

    assertTrue(exception.getMessage().contains(name));
    assertTrue(exception.getMessage().contains(Long.toString(value)));
  }
}
