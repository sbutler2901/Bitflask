package dev.sbutler.bitflask.storage.segment;

import com.google.common.collect.ImmutableList;

/**
 * Factory for creating new SegmentDeleter instances.
 */
interface SegmentDeleterFactory {

  /**
   * Creates a SegmentDeleter for deleting the provided segments.
   *
   * @param segmentsToBeDeleted the segments to be deleted by the SegmentDeleter
   * @return the created SegmentDeleter
   */
  SegmentDeleter create(ImmutableList<Segment> segmentsToBeDeleted);
}
