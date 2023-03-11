package dev.sbutler.bitflask.storage.configuration;

/**
 * How pre-existing storage files should be handled at startup.
 */
public enum StorageLoadingMode {

  LOAD,
  TRUNCATE;

  static String normalizeValue(String value) {
    return value.trim().toUpperCase();
  }
}
