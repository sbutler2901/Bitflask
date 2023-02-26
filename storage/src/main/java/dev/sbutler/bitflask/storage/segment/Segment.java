package dev.sbutler.bitflask.storage.segment;

import com.google.common.hash.BloomFilter;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import dev.sbutler.bitflask.storage.entry.Entry;
import java.util.Optional;

/**
 * Represents a single set of {@link dev.sbutler.bitflask.storage.entry.Entry}s persisted to disk.
 */
@SuppressWarnings("UnstableApiUsage")
public final class Segment {

  static final String FILE_EXTENSION = ".seg";

  private final SegmentMetadata metadata;
  private final BloomFilter<String> keyFilter;
  private final SegmentIndex segmentIndex;

  Segment(SegmentMetadata metadata, BloomFilter<String> keyFilter, SegmentIndex segmentIndex) {
    this.metadata = metadata;
    this.keyFilter = keyFilter;
    this.segmentIndex = segmentIndex;
  }

  /**
   * Returns true if this Segment <i>might</i> contain a
   * {@link dev.sbutler.bitflask.storage.entry.Entry} for the provided key or false if it
   * <i>definitely</i> does not.
   */
  public boolean mightContain(String key) {
    return keyFilter.mightContain(key) || segmentIndex.mightContain(key);
  }

  /**
   * Reads the {@link dev.sbutler.bitflask.storage.entry.Entry} contained by this Segment and
   * returns it, if present.
   */
  public ListenableFuture<Optional<Entry>> readEntry(String key) {
    return Futures.immediateFuture(Optional.empty());
  }

  /**
   * Returns the number of this Segment.
   *
   * <p>Higher numbers indicate a more recently created Segment.
   */
  public int getSegmentNumber() {
    return metadata.segmentNumber().value();
  }

  /**
   * Returns the level of this Segment.
   *
   * <p>Higher numbers indicate more rounds of compaction performed on the contained
   * {@link dev.sbutler.bitflask.storage.entry.Entry}s and therefore their age.
   */
  public int getSegmentLevel() {
    return metadata.segmentLevel().value();
  }

}
