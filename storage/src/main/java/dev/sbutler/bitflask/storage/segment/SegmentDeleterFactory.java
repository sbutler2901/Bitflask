package dev.sbutler.bitflask.storage.segment;

import java.util.List;

interface SegmentDeleterFactory {

  SegmentDeleter create(List<Segment> preCompactionSegments);
}
