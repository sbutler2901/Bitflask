package dev.sbutler.bitflask.storage;

import dev.sbutler.bitflask.storage.segment.Segment;
import dev.sbutler.bitflask.storage.segment.SegmentManager;
import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;

class StorageImpl implements Storage {

  private static final String WRITE_ERR_BAD_KEY = "Error writing data, provided key was null, empty, or longer than 256 characters";
  private static final String WRITE_ERR_BAD_VALUE = "Error writing data, provided value was null, empty, or longer than 256 characters";
  private static final String READ_ERR_BAD_KEY = "Error reading data, provided key was null, empty, or longer than 256 characters";

  private final SegmentManager segmentManager;

  public StorageImpl(SegmentManager segmentManager) {
    this.segmentManager = segmentManager;
  }

  public void write(String key, String value) throws IOException {
    validateWriteArgs(key, value);
    segmentManager.getActiveSegment().write(key, value);
  }

  private void validateWriteArgs(String key, String value) {
    if (key == null || key.length() <= 0 || key.length() > 256) {
      throw new IllegalArgumentException(WRITE_ERR_BAD_KEY);
    } else if (value == null || value.length() <= 0 || value.length() > 256) {
      throw new IllegalArgumentException(WRITE_ERR_BAD_VALUE);
    }
  }

  public Optional<String> read(String key) {
    validateReadArgs(key);

    Optional<Segment> optionalSegment = findLatestSegmentWithKey(key);
    if (optionalSegment.isEmpty()) {
      return Optional.empty();
    }

    return optionalSegment.get().read(key);
  }

  private void validateReadArgs(String key) {
    if (key == null || key.length() <= 0 || key.length() > 256) {
      throw new IllegalArgumentException(READ_ERR_BAD_KEY);
    }
  }

  /**
   * Attempts to find a key in the list of storage segments starting with the most recently created
   *
   * @param key the key to be found
   * @return the found storage segment, if one exists
   */
  private Optional<Segment> findLatestSegmentWithKey(String key) {
    Iterator<Segment> segmentIterator = segmentManager.getSegmentsIterator();
    while (segmentIterator.hasNext()) {
      Segment segment = segmentIterator.next();
      if (segment.containsKey(key)) {
        return Optional.of(segment);
      }
    }
    return Optional.empty();
  }
}
