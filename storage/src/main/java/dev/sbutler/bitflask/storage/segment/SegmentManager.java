package dev.sbutler.bitflask.storage.segment;

import java.io.IOException;
import java.util.Optional;

/**
 * Manages the various Segments used by Storage ensuring segment sizes are controlled and filesystem
 * space is conserved.
 */
public interface SegmentManager {

  /**
   * Writes the provided key and value to the current active segment.
   *
   * @param key   the key to be written
   * @param value the value to be written and associated with provided key
   * @throws IOException if there is an issue writing to the active segment
   */
  void write(String key, String value) throws IOException;

  /**
   * Attempts to find the provided key and read it's associated value.
   *
   * @param key the key to find associated value
   * @return an Optional containing the value associated with the provided key if found, an empty
   * Optional otherwise
   * @throws IOException if there is an issue finding the provided key, or reading its associated
   *                     value
   */
  Optional<String> read(String key) throws IOException;

  /**
   * Closes all Segments currently being managed
   */
  void close();

}
