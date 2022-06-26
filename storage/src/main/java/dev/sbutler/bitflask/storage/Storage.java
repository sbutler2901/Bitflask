package dev.sbutler.bitflask.storage;

import java.util.Optional;
import java.util.concurrent.Future;

/**
 * Manages persisting and retrieving data
 */
public interface Storage {

  /**
   * Writes the provided data to the current segment file
   *
   * @param key   the key for retrieving data once written. Expected to be a non-blank string.
   * @param value the data to be written. Expected to be a non-blank string.
   * @throws IllegalArgumentException when the provided key or value is invalid
   */
  Future<Void> write(String key, String value);

  /**
   * Reads the provided key's value from storage
   *
   * @param key the key used for retrieving stored data. Expected to be a non-blank string.
   * @return the read value, if found
   */
  Future<Optional<String>> read(String key);

  void shutdown() throws InterruptedException;
}
