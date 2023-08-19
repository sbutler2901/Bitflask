package dev.sbutler.bitflask.storage.commands;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link CommandInputValidator}. */
public class CommandInputValidatorTest {

  @Test
  public void validateKey_null_throws() {
    assertThrows(NullPointerException.class, () -> CommandInputValidator.validateKey(null));
  }

  @Test
  public void validateKey_blank_throws() {
    // empty
    assertThrows(IllegalArgumentException.class, () -> CommandInputValidator.validateKey(""));
    // whitespace
    assertThrows(IllegalArgumentException.class, () -> CommandInputValidator.validateKey(" "));
  }

  @Test
  public void validateKey_tooLong_throws() {
    char[] chars = new char[CommandInputValidator.KEY_MAX_SIZE + 1];
    // prevent blank check
    chars[0] = 'a';

    assertThrows(
        IllegalArgumentException.class,
        () -> CommandInputValidator.validateKey(String.valueOf(chars)));
  }

  @Test
  public void validateValue_null_throws() {
    assertThrows(NullPointerException.class, () -> CommandInputValidator.validateValue(null));
  }

  @Test
  public void validateValue_blank_throws() {
    // empty
    assertThrows(IllegalArgumentException.class, () -> CommandInputValidator.validateValue(""));
    // whitespace
    assertThrows(IllegalArgumentException.class, () -> CommandInputValidator.validateValue(" "));
  }

  @Test
  public void validateValue_tooLong_throws() {
    char[] chars = new char[CommandInputValidator.VALUE_MAX_SIZE + 1];
    // prevent blank check
    chars[0] = 'a';

    assertThrows(
        IllegalArgumentException.class,
        () -> CommandInputValidator.validateKey(String.valueOf(chars)));
  }
}
