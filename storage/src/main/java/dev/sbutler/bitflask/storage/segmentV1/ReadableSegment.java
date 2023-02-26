package dev.sbutler.bitflask.storage.segmentV1;

import java.io.IOException;
import java.util.Optional;

/**
 * Represents a Storage Segment whose data can be read.
 */
public interface ReadableSegment {

  /**
   * Reads the provided key's value, if present.
   *
   * @throws SegmentClosedException when called after the Segment has been closed
   * @throws IOException            when a general I/O error occurs
   */
  Optional<String> read(String key) throws IOException;

  /**
   * Checks if the Segment contains the provided key.
   */
  boolean containsKey(String key);

  /**
   * Returns the Segment's file's key.
   */
  int getSegmentFileKey();

  /**
   * Closes the Segment for reading and writing
   */
  void close();

  /**
   * Checks if the segment is open and able to be read or written
   */
  boolean isOpen();
}
