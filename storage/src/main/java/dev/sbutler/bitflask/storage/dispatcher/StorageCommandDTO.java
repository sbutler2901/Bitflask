package dev.sbutler.bitflask.storage.dispatcher;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public sealed interface StorageCommandDTO {

  record ReadDTO(String key) implements StorageCommandDTO {

    public ReadDTO {
      validateKey(key);
    }

    private static void validateKey(String key) {
      checkNotNull(key);
      checkArgument(!key.isBlank(), "Expected non-blank key, but was [%s]", key);
      checkArgument(key.length() <= 256, "Expect key smaller than 256 characters, but was [%d]",
          key.length());
    }
  }

  record WriteDTO(String key, String value) implements StorageCommandDTO {

    public WriteDTO {
      validateKey(key);
      validateValue(value);
    }

    private static void validateKey(String key) {
      checkArgument(!key.isBlank(), "Expected non-blank key, but was [%s]", key);
      checkArgument(key.length() <= 256, "Expect key smaller than 256 characters, but was [%d]",
          key.length());
    }

    private static void validateValue(String value) {

    }
  }
}
