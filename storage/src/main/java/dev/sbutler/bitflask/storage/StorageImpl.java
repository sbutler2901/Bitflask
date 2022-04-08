package dev.sbutler.bitflask.storage;

import java.io.IOException;
import java.util.ListIterator;
import java.util.Optional;

/**
 * Manages persisting and retrieving data
 */
class StorageImpl implements Storage {

  private static final String WRITE_ERR_BAD_KEY = "Error writing data, provided key was null or empty";
  private static final String WRITE_ERR_BAD_VALUE = "Error writing data, provided value was null or empty";
  private static final String READ_ERR_BAD_KEY = "Error reading data, provided key was null or empty";

  private final StorageSegmentManager storageSegmentManager;

  public StorageImpl(StorageSegmentManager storageSegmentManager) {
    this.storageSegmentManager = storageSegmentManager;
  }

  /**
   * Writes the provided data to the current segment file
   *
   * @param key   the key for retrieving data once written. Expected to be a non-blank string.
   * @param value the data to be written. Expected to be a non-blank string.
   * @throws IOException              when creating a new segment file fails
   * @throws IllegalArgumentException when the provided key or value is invalid
   */
  public void write(String key, String value) throws IOException {
    validateWriteArgs(key, value);
    storageSegmentManager.getActiveSegment().write(key, value);
  }

  private void validateWriteArgs(String key, String value) {
    if (key == null || key.length() <= 0) {
      throw new IllegalArgumentException(WRITE_ERR_BAD_KEY);
    } else if (value == null || value.length() <= 0) {
      throw new IllegalArgumentException(WRITE_ERR_BAD_VALUE);
    }
  }

  /**
   * Reads the provided key's value from storage
   *
   * @param key the key used for retrieving stored data. Expected to be a non-blank string.
   * @return the read value, if found
   */
  public Optional<String> read(String key) {
    validateReadArgs(key);

    Optional<StorageSegment> optionalStorageSegment = findLatestSegmentWithKey(key);
    if (optionalStorageSegment.isEmpty()) {
      return Optional.empty();
    }

    return optionalStorageSegment.get().read(key);
  }

  private void validateReadArgs(String key) {
    if (key == null || key.length() <= 0) {
      throw new IllegalArgumentException(READ_ERR_BAD_KEY);
    }
  }

  /**
   * Attempts to find a key in the list of storage segments starting with the most recently created
   *
   * @param key the key to be found
   * @return the found storage segment, if one exists
   */
  private Optional<StorageSegment> findLatestSegmentWithKey(String key) {
    ListIterator<StorageSegment> segmentListIterator = storageSegmentManager.getStorageSegmentsIteratorReversed();
    while (segmentListIterator.hasPrevious()) {
      StorageSegment storageSegment = segmentListIterator.previous();
      if (storageSegment.containsKey(key)) {
        return Optional.of(storageSegment);
      }
    }
    return Optional.empty();
  }
}
