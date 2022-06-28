package dev.sbutler.bitflask.storage.segment;

import java.io.IOException;
import java.nio.file.Path;

interface SegmentFactory {

  /**
   * Creates a new Segment, and it's associate SegmentFile in the filesystem
   *
   * @return the created Segment
   * @throws IOException if an error occurs while creating the segment
   */
  Segment createSegment() throws IOException;

  /**
   * Creates a segment from a preexisting SegmentFile. This does not increment the key used for
   * creating new Segments.
   *
   * @param segmentFile the associated SegmentFile for the Segment being created
   * @return the created segment
   * @throws IOException if an error occurs while creating the segment
   */
  Segment createSegmentFromFile(SegmentFile segmentFile) throws IOException;

  /**
   * Sets the key that is used as the file key when creating new Segments. This is incremented with
   * each new Segment while a SegmentFile is created.
   *
   * @param segmentStartKey the key to be used on next segment creation
   */
  void setSegmentStartKey(int segmentStartKey);

  /**
   * Creates the segment store directory if it doesn't exist.
   *
   * @return true if the directory was created
   */
  boolean createSegmentStoreDir() throws IOException;

  /**
   * Provides the Path to the directory used for storing Segments' files
   *
   * @return the Path to the directory used to store Segments
   */
  Path getSegmentStoreDirPath();

  /**
   * Retrieves the key for a segment from its file path.
   *
   * @param path the path to retrieve the segment key from
   * @return the Segment's Key.
   */
  int getSegmentKeyFromPath(Path path);

}
