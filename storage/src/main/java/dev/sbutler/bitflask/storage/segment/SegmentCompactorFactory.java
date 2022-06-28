package dev.sbutler.bitflask.storage.segment;

import com.google.common.collect.ImmutableList;

/**
 * Factory for creating new SegmentCompactor instances.
 */
interface SegmentCompactorFactory {

  /**
   * Creates a SegmentCompactor for compacting the provided segments. The provided Segments should
   * be in order from most recently written to the earliest written.
   *
   * @param segmentsToBeCompacted the segments that should be compacted by the created instance
   * @return the created SegmentCompactor
   */
  SegmentCompactor create(ImmutableList<Segment> segmentsToBeCompacted);
}
