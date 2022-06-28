package dev.sbutler.bitflask.storage.segment;

import com.google.common.collect.ImmutableList;
import java.io.IOException;

interface SegmentLoader {

  /**
   * Loads preexisting segments from the filesystem and initializes them for usage. Assumes the
   * directory for storing segments exists.
   *
   * @return the loaded segments
   * @throws IOException if an error occurs while loading the segments
   */
  ImmutableList<Segment> loadExistingSegments() throws IOException;

}
