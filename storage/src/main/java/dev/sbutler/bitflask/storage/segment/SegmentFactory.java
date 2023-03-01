package dev.sbutler.bitflask.storage.segment;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.common.primitives.UnsignedShort;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.entry.Entry;
import dev.sbutler.bitflask.storage.entry.EntryReader;
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
  private final EntryReader.Factory entryReaderFactory;
  private final AtomicInteger nextSegmentNumber;

  private SegmentFactory(ListeningExecutorService executorService,
      StorageConfigurations configurations,
      EntryReader.Factory entryReaderFactory,
      int nextSegmentNumber) {
    this.executorService = executorService;
    this.configurations = configurations;
    this.entryReaderFactory = entryReaderFactory;
    this.nextSegmentNumber = new AtomicInteger(nextSegmentNumber);
  }

  /**
   * A factory for creating {@link SegmentFactory instances}.
   */
  public static class Factory {

    private final ListeningExecutorService executorService;
    private final StorageConfigurations configurations;
    private final EntryReader.Factory entryReaderFactory;

    @Inject
    Factory(ListeningExecutorService executorService,
        StorageConfigurations configurations,
        EntryReader.Factory entryReaderFactory) {
      this.executorService = executorService;
      this.configurations = configurations;
      this.entryReaderFactory = entryReaderFactory;
    }

    /**
     * Creates a {@link SegmentFactory} that will create new segments starting from the provided
     * {@code segmentNumberStart}.
     */
    public SegmentFactory create(int segmentNumberStart) {
      return new SegmentFactory(executorService, configurations,
          entryReaderFactory, segmentNumberStart);
    }
  }

  /**
   * Creates a new Segment at segment level 0 and its associated index file.
   */
  public ListenableFuture<Segment> create(ImmutableSortedMap<String, Entry> keyEntryMap) {
    return Futures.submit(() -> {
      UnsignedShort segmentNumber = UnsignedShort.valueOf(nextSegmentNumber.getAndIncrement());

      SegmentIndex segmentIndex = createSegmentIndex(keyEntryMap, segmentNumber);
      return createSegment(keyEntryMap, segmentNumber, segmentIndex);
    }, executorService);
  }

  /**
   * Creates a new {@link Segment} at segment level 0.
   */
  Segment createSegment(ImmutableSortedMap<String, Entry> keyEntryMap,
      UnsignedShort segmentNumber, SegmentIndex segmentIndex) throws IOException {

    SegmentMetadata segmentMetadata = new SegmentMetadata(
        segmentNumber, UnsignedShort.valueOf(0));
    BloomFilter<String> keyFilter = BloomFilter.create(Funnels.stringFunnel(
        StandardCharsets.UTF_8), keyEntryMap.size());

    Path segmentPath = Path.of(configurations.getStorageStoreDirectoryPath().toString(),
        Segment.createFileName(segmentNumber.value()));

    try (BufferedOutputStream segmentOutputStream = new BufferedOutputStream(
        Files.newOutputStream(segmentPath, StandardOpenOption.CREATE_NEW))) {

      byte[] segmentMetadataBytes = segmentMetadata.getBytes();
      segmentOutputStream.write(segmentMetadataBytes);

      for (Entry entry : keyEntryMap.values()) {
        segmentOutputStream.write(entry.getBytes());

        keyFilter.put(entry.key());
      }
    }

    EntryReader entryReader = entryReaderFactory.create(segmentPath);

    return Segment.create(segmentMetadata, entryReader, keyFilter, segmentIndex);
  }

  /**
   * Creates a new {@link SegmentIndex}.
   */
  SegmentIndex createSegmentIndex(ImmutableSortedMap<String, Entry> keyEntryMap,
      UnsignedShort segmentNumber) throws IOException {
    SegmentIndexMetadata indexMetadata = new SegmentIndexMetadata(segmentNumber);
    ImmutableSortedMap.Builder<String, Long> indexKeyOffsetMap = ImmutableSortedMap.naturalOrder();

    Path indexPath = Path.of(configurations.getStorageStoreDirectoryPath().toString(),
        SegmentIndex.createFileName(segmentNumber.value()));

    try (BufferedOutputStream indexOutputStream = new BufferedOutputStream(
        Files.newOutputStream(indexPath, StandardOpenOption.CREATE_NEW))) {

      indexOutputStream.write(indexMetadata.getBytes());

      long segmentOffset = SegmentMetadata.BYTES;
      for (Entry entry : keyEntryMap.values()) {
        SegmentIndexEntry indexEntry = new SegmentIndexEntry(entry.key(), segmentOffset);
        indexOutputStream.write(indexEntry.getBytes());
        indexKeyOffsetMap.put(entry.key(), segmentOffset);
        segmentOffset += entry.getBytes().length;
      }
    }

    return new SegmentIndexDense(indexMetadata, indexKeyOffsetMap.build());
  }
}
