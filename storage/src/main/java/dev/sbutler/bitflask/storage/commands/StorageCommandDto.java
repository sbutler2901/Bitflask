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
public sealed interface StorageCommandDto {

  /**
   * Indicates if this DTO represents a command that is results in persistence changes and should be
   * replicated.
   */
  boolean isPersistable();

  /** Use when the value mapped by the provided {@code key} should be read. */
  record ReadDto(String key) implements StorageCommandDto {

    public ReadDto {
      CommandInputValidator.validateKey(key);
    }

    @Override
    public boolean isPersistable() {
      return false;
    }
  }

  /**
   * Use when the provided {@code key} should be written with a mapping to the provided {@code
   * value}.
   */
  record WriteDto(String key, String value) implements StorageCommandDto {

    public WriteDto {
      CommandInputValidator.validateKey(key);
      CommandInputValidator.validateValue(value);
    }

    @Override
    public boolean isPersistable() {
      return true;
    }
  }

  /** Use when the provides {@code key}'s mapping should be deleted. */
  record DeleteDto(String key) implements StorageCommandDto {

    public DeleteDto {
      CommandInputValidator.validateKey(key);
    }

    @Override
    public boolean isPersistable() {
      return true;
    }
  }
}
