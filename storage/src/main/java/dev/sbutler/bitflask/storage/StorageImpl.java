package dev.sbutler.bitflask.storage;

import dev.sbutler.bitflask.storage.segment.SegmentImpl;
import dev.sbutler.bitflask.storage.segment.SegmentManagerImpl;
import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;

/**
 * Manages persisting and retrieving data
 */
class StorageImpl implements Storage {

  private static final String WRITE_ERR_BAD_KEY = "Error writing data, provided key was null or empty";
  private static final String WRITE_ERR_BAD_VALUE = "Error writing data, provided value was null or empty";
  private static final String READ_ERR_BAD_KEY = "Error reading data, provided key was null or empty";

  private final SegmentManagerImpl segmentManager;

  public StorageImpl(SegmentManagerImpl segmentManager) {
    this.segmentManager = segmentManager;
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
    segmentManager.getActiveSegment().write(key, value);
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

    Optional<SegmentImpl> optionalStorageSegment = findLatestSegmentWithKey(key);
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
  private Optional<SegmentImpl> findLatestSegmentWithKey(String key) {
    Iterator<SegmentImpl> segmentIterator = segmentManager.getStorageSegmentsIterator();
    while (segmentIterator.hasNext()) {
      SegmentImpl segment = segmentIterator.next();
      if (segment.containsKey(key)) {
        return Optional.of(segment);
      }
    }
    return Optional.empty();
  }
}
