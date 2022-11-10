package dev.sbutler.bitflask.storage.segment;

import java.io.IOException;
import java.util.Optional;

/**
 * Represents a base Storage Segment with general info available and data available for reading.
 */
public interface ReadableSegment {

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
}
