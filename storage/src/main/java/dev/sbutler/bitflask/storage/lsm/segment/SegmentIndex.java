package dev.sbutler.bitflask.storage.lsm.segment;

import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import java.nio.file.Path;
import java.util.Optional;

/**
 * An index of {@link Entry} offsets for a {@link Segment}.
 */
interface SegmentIndex {

  /**
   * The prefix of SegmentIndex files.
   */
  String FILE_PREFIX = "index_";
  /**
   * The file extension used to identify index files.
   */
  String FILE_EXTENSION = "idx";

  /**
   * Returns true if the index <i>might</i> contain the provided key or false if it
   * <i>definitely</i> does not.
   */
  boolean mightContain(String key);

  /**
   * Returns the file offset to start searching for a {@link Entry} in a {@link Segment}, if
   * present.
   */
  Optional<Long> getKeyOffset(String key);

  /**
   * The number of the {@link Segment} to which this index corresponds.
   */
  int getSegmentNumber();

  /**
   * Creates the file name for a SegmentIndex with {@code segmentNumber}.
   */
  static String createFileName(int segmentNumber) {
    return FILE_PREFIX + segmentNumber + "." + FILE_EXTENSION;
  }

  /**
   * Gets the SegmentIndex's file path.
   */
  Path getFilePath();
}
