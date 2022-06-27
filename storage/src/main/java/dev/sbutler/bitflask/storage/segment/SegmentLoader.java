package dev.sbutler.bitflask.storage.segment;

import com.google.common.collect.ImmutableList;
import java.io.IOException;

interface SegmentLoader {

  ImmutableList<Segment> loadExistingSegments() throws IOException;

}
