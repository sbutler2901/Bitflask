package dev.sbutler.bitflask.storage.segment;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

/**
 * Represents a single self-contained file for storing data
 */
interface Segment {

  /**
   * Writes the provided key and value to the segment file
   *
   * @param key   the key to be written and saved for retrieving data
   * @param value the associated data value to be written
   */
  void write(String key, String value) throws IOException;

  /**
   * Reads the provided key's value from the segment file
   *
   * @param key the key to find the data in the segment file
   * @return the value for the key from the segment file, if it exists
   */
  Optional<String> read(String key) throws IOException;

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

  /**
   * Returns all keys stored by the segment
   *
   * @return a set of the keys stored by the segment
   */
  Set<String> getSegmentKeys();

  /**
   * Returns the segment's file's key
   *
   * @return the segment's file's key
   */
  int getSegmentFileKey();

  /**
   * Deletes the segment from the filesystem
   */
  void closeAndDelete() throws IOException;

  /**
   * Marks the segments as frozen, preventing any further writes
   */
  void markFrozen();

  /**
   * Checks if the segment has been marked as frozen
   *
   * @return whether the segment is frozen or not
   */
  boolean isFrozen();

  /**
   * Marks the segment as compacted
   */
  void markCompacted();

  /**
   * Checks if a segment has been marked as compacted
   *
   * @return whether the segment has been compacted or not
   */
  boolean hasBeenCompacted();

  /**
   * Checks if the file has been close and no longer able to be read or written
   *
   * @return whether the segment has been closed or not
   */
  boolean isClosed();
}
