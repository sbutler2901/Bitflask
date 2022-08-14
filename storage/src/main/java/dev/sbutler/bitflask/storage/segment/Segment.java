package dev.sbutler.bitflask.storage.segment;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;

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
  ImmutableSet<String> getSegmentKeys();

  /**
   * Returns the segment's file's key
   *
   * @return the segment's file's key
   */
  int getSegmentFileKey();

  /**
   * Closes the segment for reading and writing
   */
  void close();

  /**
   * Checks if the segment is open and able to be read or written
   *
   * @return whether the segment is open or not
   */
  boolean isOpen();

  /**
   * Deletes the segment from the filesystem
   *
   * @throws IOException if there is an issue deleting the segment
   */
  void delete() throws IOException;

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

  void registerSizeLimitExceededConsumer(Consumer<Segment> sizeLimitExceededConsumer);
}
