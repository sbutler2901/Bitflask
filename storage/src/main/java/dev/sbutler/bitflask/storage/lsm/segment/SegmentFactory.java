package dev.sbutler.bitflask.storage.lsm.segment;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
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
public final class SegmentFactory {

  private final ListeningExecutorService executorService;
  private final StorageConfigurations configurations;
  private final SegmentIndexFactory indexFactory;
  private final AtomicInteger nextSegmentNumber;

  private SegmentFactory(ListeningExecutorService executorService,
      StorageConfigurations configurations,
      SegmentIndexFactory indexFactory,
      int nextSegmentNumber) {
    this.executorService = executorService;
    this.configurations = configurations;
    this.indexFactory = indexFactory;
    this.nextSegmentNumber = new AtomicInteger(nextSegmentNumber);
  }

  /**
   * A factory for creating {@link SegmentFactory instances}.
   */
  public static class Factory {

    private final ListeningExecutorService executorService;
    private final StorageConfigurations configurations;
    private final SegmentIndexFactory indexFactory;

    @Inject
    Factory(ListeningExecutorService executorService,
        StorageConfigurations configurations,
        SegmentIndexFactory indexFactory) {
      this.executorService = executorService;
      this.configurations = configurations;
      this.indexFactory = indexFactory;
    }

    /**
     * Creates a {@link SegmentFactory} that will create new segments starting from the provided
     * {@code segmentNumberStart}.
     */
    public SegmentFactory create(int segmentNumberStart) {
      return new SegmentFactory(executorService,
          configurations,
          indexFactory,
          segmentNumberStart);
    }
  }

  /**
   * Creates a new Segment at segment level 0 and its associated index file.
   *
   * <p>The provided {@code keyEntryMap} cannot be empty.
   */
  public ListenableFuture<Segment> create(ImmutableSortedMap<String, Entry> keyEntryMap) {
    checkArgument(!keyEntryMap.isEmpty(), "keyEntryMap was negative.");

    return Futures.submit(() -> createSegmentAndIndex(keyEntryMap), executorService);
  }

  Segment createSegmentAndIndex(ImmutableSortedMap<String, Entry> keyEntryMap) throws IOException {
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
}
