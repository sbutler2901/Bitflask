package dev.sbutler.bitflask.storage.dispatcher;

/**
 * The various commands that the storage engine can accept.
 *
 * <p>Input will be validated at construction, throwing an {@link NullPointerException} if a
 * provided string was null, or {@link IllegalArgumentException} if the string is blank or too
 * long.
 *
 * <p>A key or value must not be blank, defined by {@code string.isBlank()}. A key cannot be longer
 * than 256, and a value no longer than 1024.
 */
public sealed interface StorageCommandDTO {

  /**
   * Use when the value mapped by the provided {@code key} should be read.
   */
  record ReadDTO(String key) implements StorageCommandDTO {

    public ReadDTO {
      InputValidator.validateKey(key);
    }
  }

  /**
   * Use when the provided {@code key} should be written with a mapping to the provided
   * {@code value}.
   */
  record WriteDTO(String key, String value) implements StorageCommandDTO {

    public WriteDTO {
      InputValidator.validateKey(key);
      InputValidator.validateValue(value);
    }
  }
}
