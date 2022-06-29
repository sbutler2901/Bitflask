package dev.sbutler.bitflask.storage.segment;

import java.nio.channels.FileChannel;
import java.nio.file.Path;

/**
 * Factory for creating new SegmentFile instances.
 */
interface SegmentFileFactory {

  /**
   * Creates a SegmentFile.
   *
   * @param segmentFileChannel File channel for storing segment data
   * @param segmentFilePath    path of the segment file
   * @param segmentFileKey     key of the segment file
   * @return the created SegmentFile
   */
  SegmentFile create(FileChannel segmentFileChannel, Path segmentFilePath, int segmentFileKey);
}
