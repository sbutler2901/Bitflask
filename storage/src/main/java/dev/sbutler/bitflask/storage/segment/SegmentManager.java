package dev.sbutler.bitflask.storage.segment;

import java.io.IOException;
import java.util.Iterator;

public interface SegmentManager {

  /**
   * Gets the active Segment for use by the storage engine
   *
   * @return the active Segment
   * @throws IOException when a new segment should be created and made active
   */
  Segment getActiveSegment() throws IOException;

  /**
   * Gets an iterator of all managed segments
   *
   * @return an iterator of all managed segments
   * @throws UnsupportedOperationException if a modification is attempted via the iterator
   */
  Iterator<Segment> getSegmentsIterator();

}
