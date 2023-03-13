package dev.sbutler.bitflask.storage.lsm.segment;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import dev.sbutler.bitflask.common.primitives.UnsignedShort;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import dev.sbutler.bitflask.storage.lsm.entry.EntryReader;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;

/**
 * Handles the creation of a {@link Segment}.
 */
@SuppressWarnings("UnstableApiUsage")
final class SegmentFactory {

  private static final AtomicInteger nextSegmentNumber = new AtomicInteger(0);

  private final StorageConfigurations configurations;
  private final SegmentIndexFactory indexFactory;

  @Inject
  SegmentFactory(StorageConfigurations configurations, SegmentIndexFactory indexFactory) {
    this.configurations = configurations;
    this.indexFactory = indexFactory;
  }

  /**
   * Creates a new Segment at segment level 0 and its associated index file.
   *
   * <p>The provided {@code keyEntryMap} cannot be empty.
   */
  public Segment create(ImmutableSortedMap<String, Entry> keyEntryMap) throws IOException {
    checkArgument(!keyEntryMap.isEmpty(), "keyEntryMap is empty.");

    UnsignedShort segmentNumber = UnsignedShort.valueOf(nextSegmentNumber.getAndIncrement());

    SegmentMetadata segmentMetadata = new SegmentMetadata(
        segmentNumber, UnsignedShort.valueOf(0));
    Path segmentPath = Path.of(configurations.getStorageStoreDirectoryPath().toString(),
        Segment.createFileName(segmentNumber.value()));
    BloomFilter<String> keyFilter = BloomFilter.create(Funnels.stringFunnel(
        StandardCharsets.UTF_8), keyEntryMap.size());

    ImmutableSortedMap<String, Long> keyOffsetMap =
        writeSegment(keyEntryMap, segmentMetadata, keyFilter, segmentPath);

    SegmentIndex segmentIndex = indexFactory.create(keyOffsetMap, segmentNumber);

    return Segment.create(segmentMetadata, EntryReader.create(segmentPath),
        keyFilter, segmentIndex);
  }

  /**
   * Writes a new {@link Segment} to disk.
   *
   * @return a key offset map for entries in the new Segment.
   */
  ImmutableSortedMap<String, Long> writeSegment(ImmutableSortedMap<String, Entry> keyEntryMap,
      SegmentMetadata segmentMetadata, BloomFilter<String> keyFilter, Path segmentPath)
      throws IOException {

    ImmutableSortedMap.Builder<String, Long> keyOffsetMap = ImmutableSortedMap.naturalOrder();

    try (BufferedOutputStream segmentOutputStream = new BufferedOutputStream(
        Files.newOutputStream(segmentPath, StandardOpenOption.CREATE_NEW))) {

      byte[] segmentMetadataBytes = segmentMetadata.getBytes();
      segmentOutputStream.write(segmentMetadataBytes);

      long entryOffset = segmentMetadataBytes.length;
      for (Entry entry : keyEntryMap.values()) {
        keyOffsetMap.put(entry.key(), entryOffset);
        keyFilter.put(entry.key());

        byte[] entryBytes = entry.getBytes();
        segmentOutputStream.write(entryBytes);

        entryOffset += entryBytes.length;
      }
    }

    return keyOffsetMap.build();
  }

  /**
   * Loads a {@link Segment} from the path and finds its corresponding {@link SegmentIndex} from the
   * segmentNumberToIndexMap.
   */
  public Segment loadFromPath(Path path,
      ImmutableMap<Integer, SegmentIndex> segmentNumberToIndexMap) throws IOException {
    // TODO: load metadata and bloom filter from entries
    return null;
  }
}
