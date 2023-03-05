package dev.sbutler.bitflask.storage.lsm.segment;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.util.concurrent.Futures.immediateFuture;

import com.google.common.hash.BloomFilter;
import com.google.common.util.concurrent.ListenableFuture;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import dev.sbutler.bitflask.storage.lsm.entry.EntryReader;
import java.util.Optional;

/**
 * Represents a single set of {@link Entry}s persisted to disk.
 */
@SuppressWarnings("UnstableApiUsage")
public final class Segment {

  /**
   * The prefix of Segment files.
   */
  public static final String FILE_PREFIX = "segment_";
  /**
   * The file extension of Segment files.
   */
  public static final String FILE_EXTENSION = ".seg";

  private final SegmentMetadata metadata;
  private final EntryReader entryReader;
  private final BloomFilter<String> keyFilter;
  private final SegmentIndex segmentIndex;

  private Segment(SegmentMetadata metadata,
      EntryReader entryReader,
      BloomFilter<String> keyFilter,
      SegmentIndex segmentIndex) {
    this.metadata = metadata;
    this.entryReader = entryReader;
    this.keyFilter = keyFilter;
    this.segmentIndex = segmentIndex;
  }

  static Segment create(SegmentMetadata metadata,
      EntryReader entryReader,
      BloomFilter<String> keyFilter,
      SegmentIndex segmentIndex) {
    checkArgument(metadata.getSegmentNumber() == segmentIndex.getSegmentNumber(),
        "SegmentMetadata segmentNumber does not match SegmentIndex segmentNumber. [%s], [%s]",
        metadata.getSegmentNumber(), segmentIndex.getSegmentNumber());

    return new Segment(metadata, entryReader, keyFilter, segmentIndex);
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
   * {@link Entry}s and therefore their age.
   */
  public int getSegmentLevel() {
    return metadata.segmentLevel().value();
  }

  /**
   * Returns true if this Segment <i>might</i> contain a {@link Entry} for the provided key or false
   * if it
   * <i>definitely</i> does not.
   */
  public boolean mightContain(String key) {
    return keyFilter.mightContain(key) || segmentIndex.mightContain(key);
  }

  /**
   * Reads the {@link Entry} contained by this Segment and returns it, if present.
   */
  public ListenableFuture<Optional<Entry>> readEntry(String key) {
    if (!mightContain(key)) {
      return immediateFuture(Optional.empty());
    }
    return segmentIndex.getKeyOffset(key)
        .map(startOffset -> entryReader.findEntryFromOffset(key, startOffset))
        .orElseGet(() -> immediateFuture(Optional.empty()));
  }

  /**
   * Creates the file name for a Segment with {@code segmentNumber}.
   */
  public static String createFileName(int segmentNumber) {
    return FILE_PREFIX + segmentNumber + FILE_EXTENSION;
  }
}
