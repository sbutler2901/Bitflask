package dev.sbutler.bitflask.storage.segment;

import com.google.common.collect.ImmutableList;

interface SegmentDeleterFactory {

  SegmentDeleter create(ImmutableList<Segment> preCompactionSegments);
}
