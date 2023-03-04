package dev.sbutler.bitflask.storage.memtable;

import java.util.Optional;

/**
 * An in memory store of new or updated {@link dev.sbutler.bitflask.storage.entry.Entry}s.
 */
public final class Memtable {

  /**
   * Reads the value corresponding to the provided key, if present.
   */
  public Optional<String> read(String key) {
    return Optional.empty();
  }

  /**
   * Writes the provided key value pair.
   */
  public void write(String key, String value) {

  }

  /**
   * Deletes the provided key and its associated value.
   */
  public void delete(String key) {

  }
}
