package dev.sbutler.bitflask.storage.segmentV1;

import java.io.IOException;

/**
 * Checked exception thrown when an attempt is made to invoke or complete an I/O operation upon a
 * Segment that is closed.
 */
public class SegmentClosedException extends IOException {

  public SegmentClosedException(String message) {
    super(message);
  }
}
