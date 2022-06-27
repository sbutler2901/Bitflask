package dev.sbutler.bitflask.storage.segment;

import com.google.common.collect.ImmutableList;

interface SegmentCompactorFactory {

  SegmentCompactor create(ImmutableList<Segment> preCompactionSegments);
}
