package dev.sbutler.bitflask.storage.segment;

import java.io.IOException;
import java.util.Deque;

interface SegmentLoader {

  Deque<Segment> loadExistingSegments() throws IOException;

}
