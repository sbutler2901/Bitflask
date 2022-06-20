package dev.sbutler.bitflask.storage.segment;

import java.util.List;

interface SegmentCompactorFactory {

  SegmentCompactor create(List<Segment> preCompactionSegments);
}
