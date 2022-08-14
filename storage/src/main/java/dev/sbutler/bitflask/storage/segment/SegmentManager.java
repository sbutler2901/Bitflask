package dev.sbutler.bitflask.storage.segment;

import com.google.common.collect.ImmutableList;
import java.io.IOException;

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
   * Closes all Segments currently being managed
   */
  void close();

  interface ManagedSegments {

    Segment getWritableSegment();

    ImmutableList<Segment> getFrozenSegments();
  }
}
