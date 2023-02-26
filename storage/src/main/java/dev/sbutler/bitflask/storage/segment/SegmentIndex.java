package dev.sbutler.bitflask.storage.segment;

import java.util.Optional;

/**
 * An index of {@link dev.sbutler.bitflask.storage.entry.Entry} offsets for a {@link Segment}.
 */
interface SegmentIndex {

  /**
   * The file extension used to identify index files.
   */
  String FILE_EXTENSION = ".idx";

  /**
   * Returns true if the index <i>might</i> contain the provided key or false if it
   * <i>definitely</i> does not.
   */
  boolean mightContain(String key);

  /**
   * Returns the file offset of the {@link dev.sbutler.bitflask.storage.entry.Entry}, if present.
   */
  Optional<Long> getKeyOffset(String key);

  /**
   * The number of the {@link Segment} to which this index corresponds.
   */
  int getSegmentNumber();

}
