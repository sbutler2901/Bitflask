package dev.sbutler.bitflask.storage.segment;

import java.io.IOException;
import java.nio.file.Path;

interface SegmentFactory {

  Segment createSegment() throws IOException;

  void setSegmentStartIndex(int segmentStartIndex);

  Path getSegmentStoreDirPath();

}
