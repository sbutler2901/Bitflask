package dev.sbutler.bitflask.storage.segment;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Optional;

/**
 * Manages the various Segments used by StorageService ensuring segment sizes are controlled and
 * filesystem space is conserved.
 */
public interface SegmentManager {

  /**
   * Initializes the SegmentManager for reading and writing.
   *
   * <p>This should always be called first before using the SegmentManager. Repeated calls are a
   * no-op.
   *
   * @throws IOException when an error occurs that prevents the SegmentManager from being property
   *                     initialized for use.
   */
  void initialize() throws IOException;

  /**
   * Provides the currently managed segments
   */
  ManagedSegments getManagedSegments();

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

  interface ManagedSegments {

    Segment getWritableSegment();

    ImmutableList<Segment> getFrozenSegments();
  }
}
