package dev.sbutler.bitflask.storage.segment;

import java.io.IOException;
import java.nio.file.Path;

interface SegmentFactory {

  Segment createSegment() throws IOException;

  Segment createSegmentFromFile(SegmentFile segmentFile) throws IOException;

  void setSegmentStartIndex(int segmentStartIndex);

  /**
   * Creates the segment store directory if it doesn't exist.
   *
   * @return true if the directory was created
   */
  boolean createSegmentStoreDir() throws IOException;

  Path getSegmentStoreDirPath();

  int getSegmentKeyFromPath(Path path);

}
