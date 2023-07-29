package dev.sbutler.bitflask.storage;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

class InputValidator {

  static final int KEY_MAX_SIZE = 255;
  static final int VALUE_MAX_SIZE = 255;

  private InputValidator() {}

  public static void validateKey(String key) {
    checkNotNull(key);
    checkArgument(!key.isBlank(), "Expected non-blank key, but was [%s]", key);
    checkArgument(
        key.length() <= KEY_MAX_SIZE,
        "Expect key smaller than [%d] characters, but was [%d]",
        KEY_MAX_SIZE,
        key.length());
  }

  public static void validateValue(String value) {
    checkNotNull(value);
    checkArgument(!value.isBlank(), "Expected non-blank value, but was [%s]", value);
    checkArgument(
        value.length() <= VALUE_MAX_SIZE,
        "Expect value smaller than [%d] characters, but was [%d]",
        VALUE_MAX_SIZE,
        value.length());
  }
}
