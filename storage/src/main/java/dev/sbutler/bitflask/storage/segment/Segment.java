package dev.sbutler.bitflask.storage.segment;

import java.util.Optional;

/**
 * Represents a single self-contained file for storing data
 */
public interface Segment {

  /**
   * Writes the provided key and value to the segment file
   *
   * @param key   the key to be written and saved for retrieving data
   * @param value the associated data value to be written
   */
  void write(String key, String value);

  /**
   * Reads the provided key's value from the segment file
   *
   * @param key the key to find the data in the segment file
   * @return the value for the key from the segment file, if it exists
   */
  Optional<String> read(String key);

  /**
   * Checks if the segment contains the provided key
   *
   * @param key the key to be searched for
   * @return whether it contains the key, or not
   */
  boolean containsKey(String key);

  /**
   * Checks if the segment exceeds the new segment threshold
   *
   * @return whether it exceeds the threshold, or not
   */
  boolean exceedsStorageThreshold();

  interface Entry {

    /**
     * The entry's offset within the segment
     *
     * @return the offset
     */
    long getSegmentFileOffset();

    /**
     * The entry's key's byte length
     *
     * @return the key's byte length
     */
    int getKeyLength();

    /**
     * The entry's value's byte length
     *
     * @return the value's byte length
     */
    int getValueLength();

    /**
     * The entry's total length including the key and value
     *
     * @return the total entry's byte length
     */
    int getTotalLength();
  }
}
