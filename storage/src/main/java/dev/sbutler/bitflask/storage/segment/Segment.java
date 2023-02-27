package dev.sbutler.bitflask.storage.segment;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.util.concurrent.Futures.immediateFuture;

import com.google.common.hash.BloomFilter;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.storage.entry.Entry;
import dev.sbutler.bitflask.storage.entry.EntryMetadata;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import javax.inject.Inject;

/**
 * Represents a single set of {@link dev.sbutler.bitflask.storage.entry.Entry}s persisted to disk.
 */
@SuppressWarnings("UnstableApiUsage")
public final class Segment {

  static final String FILE_EXTENSION = ".seg";

  private final ListeningExecutorService executorService;
  private final SegmentMetadata metadata;
  private final BloomFilter<String> keyFilter;
  private final SegmentIndex segmentIndex;
  private final Path filePath;

  private Segment(ListeningExecutorService executorService,
      SegmentMetadata metadata,
      BloomFilter<String> keyFilter,
      SegmentIndex segmentIndex,
      Path filePath) {
    this.executorService = executorService;
    this.metadata = metadata;
    this.keyFilter = keyFilter;
    this.segmentIndex = segmentIndex;
    this.filePath = filePath;
  }

  /**
   * A factory class for creating Segment instances.
   */
  static class Factory {

    private final ListeningExecutorService executorService;

    @Inject
    Factory(ListeningExecutorService executorService) {
      this.executorService = executorService;
    }

    Segment create(SegmentMetadata metadata,
        BloomFilter<String> keyFilter,
        SegmentIndex segmentIndex,
        Path filePath) {
      checkArgument(metadata.getSegmentNumber() == segmentIndex.getSegmentNumber(),
          "SegmentMetadata segmentNumber does not match SegmentIndex segmentNumber. [%s], [%s]",
          metadata.getSegmentNumber(), segmentIndex.getSegmentNumber());

      return new Segment(executorService, metadata, keyFilter, segmentIndex, filePath);
    }
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
    if (!mightContain(key)) {
      return immediateFuture(Optional.empty());
    }
    return segmentIndex.getKeyOffset(key)
        .map(startOffset -> Futures.submit(() -> findEntry(key, startOffset), executorService))
        .orElseGet(() -> immediateFuture(Optional.empty()));
  }

  /**
   * Iterates the {@link Entry}s in this Segment until one with the provided key is found, or the
   * end of the segment file is reached.
   *
   * <p>An {@link IOException} will be thrown if there is an issue iterating the entries.
   */
  private Optional<Entry> findEntry(String key, long startOffset) throws IOException {
    try (BufferedInputStream is =
        new BufferedInputStream(Files.newInputStream(filePath, StandardOpenOption.READ))) {
      is.skipNBytes(startOffset);

      byte[] metadataBuffer = new byte[EntryMetadata.BYTE_ARRAY_LENGTH];
      while (is.read(metadataBuffer) != -1) {
        EntryMetadata entryMetadata = EntryMetadata.fromBytes(metadataBuffer);

        byte[] keyBuffer = is.readNBytes(entryMetadata.getKeyLength());
        if (keyBuffer.length != entryMetadata.getKeyLength()) {
          throw new IOException(String.format(
              "Read key length did not match entry. Read [%d], expected [%d].",
              keyBuffer.length, entryMetadata.getKeyLength()));
        }
        String readKey = new String(keyBuffer);

        if (key.equals(readKey)) {
          byte[] valueBuffer = is.readNBytes(entryMetadata.getValueLength());
          if (valueBuffer.length != entryMetadata.getValueLength()) {
            throw new IOException(String.format(
                "Read value length did not match entry. Read [%d], expected [%d].",
                valueBuffer.length, entryMetadata.getValueLength()));
          }
          String value = new String(valueBuffer);

          return Optional.of(new Entry(entryMetadata.creationEpochSeconds(), key, value));
        }

        is.skipNBytes(entryMetadata.getValueLength());
      }
    }
    return Optional.empty();
  }
}
