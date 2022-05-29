package dev.sbutler.bitflask.storage.segment;

import java.io.IOException;

interface SegmentFactory {

  Segment createSegment() throws IOException;

  void setSegmentStartIndex(int segmentStartIndex);

  String getSegmentStoreFilePath();

}
