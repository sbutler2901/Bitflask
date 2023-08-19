package dev.sbutler.bitflask.storage.commands;

/**
 * The various commands that the storage engine can accept.
 *
 * <p>Input will be validated at construction, throwing an {@link NullPointerException} if a
 * provided string was null, or {@link IllegalArgumentException} if the string is blank or too long.
 *
 * <p>A key or value must not be blank, defined by {@code string.isBlank()}. A key or value cannot
 * be longer than 255 each.
 */
public sealed interface StorageCommandDTO {

  /** Use when the value mapped by the provided {@code key} should be read. */
  record ReadDTO(String key) implements StorageCommandDTO {

    public ReadDTO {
      CommandInputValidator.validateKey(key);
    }
  }

  /**
   * Use when the provided {@code key} should be written with a mapping to the provided {@code
   * value}.
   */
  record WriteDTO(String key, String value) implements StorageCommandDTO {

    public WriteDTO {
      CommandInputValidator.validateKey(key);
      CommandInputValidator.validateValue(value);
    }
  }

  /** Use when the provides {@code key}'s mapping should be deleted. */
  record DeleteDTO(String key) implements StorageCommandDTO {

    public DeleteDTO {
      CommandInputValidator.validateKey(key);
    }
  }
}
