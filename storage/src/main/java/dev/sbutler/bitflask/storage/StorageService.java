package dev.sbutler.bitflask.storage;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Optional;

/**
 * Manages persisting and retrieving data.
 */
public interface StorageService {

  /**
   * Writes the provided data to the current segment file
   *
   * @param key   the key for retrieving data once written. Expected to be a non-blank string.
   * @param value the data to be written. Expected to be a non-blank string.
   * @throws IllegalArgumentException when the provided key or value is invalid
   */
  ListenableFuture<Void> write(String key, String value);

  /**
   * Reads the provided key's value from storage
   *
   * @param key the key used for retrieving stored data. Expected to be a non-blank string.
   * @return the read value, if found
   */
  ListenableFuture<Optional<String>> read(String key);

  /**
   * Initiates the shutdown procedure cleaning up utilized resources and stopping tasks. Will wait
   * for executing tasks to complete for an unspecified amount of time. If they aren't complete
   * within the timeframe they will be canceled.
   *
   * @throws InterruptedException if shutdown is interrupted while waiting
   */
  void shutdown() throws InterruptedException;
}
