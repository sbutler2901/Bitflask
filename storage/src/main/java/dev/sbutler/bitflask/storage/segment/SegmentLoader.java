package dev.sbutler.bitflask.storage.segment;

import java.io.IOException;
import java.util.List;

interface SegmentLoader {

  List<Segment> loadExistingSegments() throws IOException;

}
