package dev.sbutler.bitflask.storage.segment;

import java.io.IOException;

/**
 * Represents a Storage Segment whose data can be modified.
 */
public interface WritableSegment extends ReadableSegment {

  /**
   * Writes the provided key and value to the segment file
   */
  void write(String key, String value) throws IOException;

  /**
   * Deletes the provided key from the segment.
   */
  void delete(String key) throws IOException;
}
