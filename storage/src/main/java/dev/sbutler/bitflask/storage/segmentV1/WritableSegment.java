package dev.sbutler.bitflask.storage.segmentV1;

import java.io.IOException;

/**
 * Represents a Storage Segment whose data can be modified.
 */
public interface WritableSegment extends ReadableSegment {

  /**
   * Writes the provided key and value to the segment file
   *
   * @throws SegmentClosedException when called after the Segment has been closed
   * @throws IOException            when a general I/O error occurs
   */
  void write(String key, String value) throws IOException;

  /**
   * Deletes the provided key from the segment.
   *
   * @throws SegmentClosedException when called after the Segment has been closed
   * @throws IOException            when a general I/O error occurs
   */
  void delete(String key) throws IOException;
}
