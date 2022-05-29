package dev.sbutler.bitflask.storage.segment;

import java.util.Deque;

interface SegmentLoader {

  Deque<Segment> loadExistingSegments();

}
