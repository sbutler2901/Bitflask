package dev.sbutler.bitflask.storage.lsm.segment;

import static com.google.common.base.Preconditions.checkArgument;
import static dev.sbutler.bitflask.storage.lsm.utils.LoaderUtils.checkLoadedBytesLength;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import dev.sbutler.bitflask.common.primitives.UnsignedShort;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.exceptions.StorageLoadException;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import dev.sbutler.bitflask.storage.lsm.entry.EntryReader;
import dev.sbutler.bitflask.storage.lsm.memtable.Memtable;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Handles the creation of a {@link Segment}.
 */
@SuppressWarnings("UnstableApiUsage")
@Singleton
public final class SegmentFactory {

  private final AtomicInteger nextSegmentNumber = new AtomicInteger(0);

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
   * <p>The provided {@link Memtable}'s {@code keyEntryMap} cannot be empty.
   */
  public Segment create(Memtable memtable) throws IOException {
    return create(memtable.flush(), 0, memtable.getNumBytesSize());
  }

  /**
   * Creates a new Segment and its associated index file at the specified segment level.
   *
   * <p>The provided {@code keyEntryMap} cannot be empty. The segmentLevel must be non-negative.
   */
  public Segment create(SortedMap<String, Entry> keyEntryMap, int segmentLevel) throws IOException {
    long numBytesSize = keyEntryMap.values().stream()
        .map(Entry::getNumBytesSize)
        .mapToLong(Long::longValue)
        .sum();
    return create(keyEntryMap, segmentLevel, numBytesSize);
  }

  /**
   * Creates a new Segment and its associated index file at the specified segment level.
   *
   * <p>The provided {@code keyEntryMap} cannot be empty. The segmentLevel and numBytesSize must be
   * non-negative.
   */
  public Segment create(SortedMap<String, Entry> keyEntryMap, int segmentLevel, long numBytesSize)
      throws IOException {
    checkArgument(!keyEntryMap.isEmpty(), "keyEntryMap is empty.");
    checkArgument(segmentLevel >= 0, "segmentLevel must be non-negative");
    checkArgument(numBytesSize >= 0, "numBytesSize must be non-negative");

    UnsignedShort segmentNumber = UnsignedShort.valueOf(nextSegmentNumber.getAndIncrement());

    SegmentMetadata segmentMetadata = new SegmentMetadata(
        segmentNumber, UnsignedShort.valueOf(0));
    Path segmentPath = Path.of(configurations.getStoreDirectoryPath().toString(),
        Segment.createFileName(segmentNumber.value()));
    BloomFilter<String> keyFilter = BloomFilter.create(Funnels.stringFunnel(
        StandardCharsets.UTF_8), keyEntryMap.size());

    SortedMap<String, Long> keyOffsetMap =
        writeSegment(keyEntryMap, segmentMetadata, keyFilter, segmentPath);

    SegmentIndex segmentIndex = indexFactory.create(keyOffsetMap, segmentNumber);

    return Segment.create(segmentMetadata, EntryReader.create(segmentPath),
        keyFilter, segmentIndex, numBytesSize);
  }

  /**
   * Writes a new {@link Segment} to disk.
   *
   * @return a key offset map for entries in the new Segment.
   */
  SortedMap<String, Long> writeSegment(SortedMap<String, Entry> keyEntryMap,
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
  Segment loadFromPath(Path path,
      ImmutableMap<Integer, SegmentIndex> segmentNumberToIndexMap) throws IOException {

    SegmentMetadata metadata;
    try (var is = Files.newInputStream(path)) {
      byte[] metadataBytes = is.readNBytes(SegmentMetadata.BYTES);
      checkLoadedBytesLength(metadataBytes, SegmentMetadata.BYTES, SegmentMetadata.class);
      metadata = SegmentMetadata.fromBytes(metadataBytes);
    }

    EntryReader entryReader = EntryReader.create(path);
    ImmutableList<Entry> entries = entryReader.readAllEntriesFromOffset(SegmentMetadata.BYTES);

    BloomFilter<String> keyFilter = BloomFilter.create(Funnels.stringFunnel(
        StandardCharsets.UTF_8), entries.size());
    entries.stream().map(Entry::key).forEach(keyFilter::put);
    long numBytesSize = entries.stream()
        .map(Entry::getNumBytesSize)
        .mapToLong(Long::longValue)
        .sum();

    SegmentIndex index = segmentNumberToIndexMap.get(metadata.getSegmentNumber());
    if (index == null) {
      throw new StorageLoadException(String.format(
          "Could not find a SegmentIndex with expected segment number [%d]",
          metadata.getSegmentNumber()));
    }

    nextSegmentNumber.getAndUpdate(current -> Math.max(1 + metadata.getSegmentNumber(), current));

    return Segment.create(metadata, entryReader, keyFilter, index, numBytesSize);
  }
}
