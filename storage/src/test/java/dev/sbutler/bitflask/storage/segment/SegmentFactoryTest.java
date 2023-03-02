package dev.sbutler.bitflask.storage.segment;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.google.common.primitives.Bytes;
import com.google.common.util.concurrent.testing.TestingExecutors;
import dev.sbutler.bitflask.common.primitives.UnsignedShort;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.entry.Entry;
import dev.sbutler.bitflask.storage.entry.EntryReader;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

@SuppressWarnings({"resource", "UnstableApiUsage"})
public class SegmentFactoryTest {

  private final Path testResourcePath = Paths.get("src/test/resources/");
  private final StorageConfigurations config = mock(StorageConfigurations.class);
  private final EntryReader.Factory entryReaderFactory = mock(EntryReader.Factory.class);

  private final Entry entry0 = new Entry(Instant.now().getEpochSecond(), "key0", "value0");
  private final Entry entry1 = new Entry(Instant.now().getEpochSecond(), "key1", "value1");

  private final UnsignedShort segmentNumber = UnsignedShort.valueOf(0);
  private final UnsignedShort segmentLevel = UnsignedShort.valueOf(0);

  private SegmentFactory factory;

  @BeforeEach
  public void beforeEach() {
    when(config.getStorageStoreDirectoryPath()).thenReturn(testResourcePath);
    factory = new SegmentFactory.Factory(TestingExecutors.sameThreadScheduledExecutor(),
        config, entryReaderFactory).create(segmentNumber.value());
  }

  @Test
  public void create() throws Exception {
    ImmutableSortedMap<String, Entry> keyEntryMap = ImmutableSortedMap.<String, Entry>naturalOrder()
        .put("key0", entry0)
        .build();

    Segment segment;

    ByteArrayOutputStream segmentOutputStream = new ByteArrayOutputStream();
    ByteArrayOutputStream indexOutputStream = new ByteArrayOutputStream();
    try (MockedStatic<Files> fileMockedStatic = mockStatic(Files.class)) {
      fileMockedStatic.when(() -> Files.newOutputStream(
              Path.of(testResourcePath.toString(), "segment_0.seg"), StandardOpenOption.CREATE_NEW))
          .thenReturn(segmentOutputStream);
      fileMockedStatic.when(() -> Files.newOutputStream(
              Path.of(testResourcePath.toString(), "index_0.idx"), StandardOpenOption.CREATE_NEW))
          .thenReturn(indexOutputStream);

      segment = factory.create(keyEntryMap).get();
    }

    assertThat(segment.getSegmentNumber()).isEqualTo(segmentNumber.value());
    assertThat(segment.getSegmentLevel()).isEqualTo(segmentLevel.value());
    assertThat(segment.mightContain("key0")).isTrue();

    assertThat(segmentOutputStream.toByteArray()).isEqualTo(Bytes.concat(
        new SegmentMetadata(segmentNumber, segmentLevel).getBytes(),
        entry0.getBytes()));

    assertThat(indexOutputStream.toByteArray()).isEqualTo(Bytes.concat(
        new SegmentIndexMetadata(segmentNumber).getBytes(),
        new SegmentIndexEntry("key0", SegmentMetadata.BYTES).getBytes()));
  }

  @Test
  public void create_emptyKeyEntryMap() {
    ImmutableSortedMap<String, Entry> keyEntryMap = ImmutableSortedMap.<String, Entry>naturalOrder()
        .build();

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> factory.create(keyEntryMap));

    assertThat(e).hasMessageThat().ignoringCase()
        .isEqualTo("keyEntryMap was negative.");
  }

  @Test
  public void writeSegment() throws Exception {
    ImmutableSortedMap<String, Entry> keyEntryMap = ImmutableSortedMap.<String, Entry>naturalOrder()
        .put("key0", entry0)
        .put("key1", entry1)
        .build();
    SegmentMetadata metadata = new SegmentMetadata(segmentNumber, segmentLevel);
    BloomFilter<String> keyFilter = BloomFilter.create(Funnels.stringFunnel(
        StandardCharsets.UTF_8), keyEntryMap.size());

    ImmutableSortedMap<String, Long> keyOffsetMap;

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try (MockedStatic<Files> fileMockedStatic = mockStatic(Files.class)) {
      fileMockedStatic.when(() -> Files.newOutputStream(any(), any())).thenReturn(outputStream);

      keyOffsetMap = factory.writeSegment(keyEntryMap, metadata, keyFilter, testResourcePath);
    }

    assertThat(keyFilter.mightContain("key0")).isTrue();
    assertThat(keyFilter.mightContain("key1")).isTrue();

    assertThat(outputStream.toByteArray()).isEqualTo(Bytes.concat(
        metadata.getBytes(),
        entry0.getBytes(),
        entry1.getBytes()));

    assertThat(keyOffsetMap.get("key0")).isEqualTo(SegmentMetadata.BYTES);
    assertThat(keyOffsetMap.get("key1")).isEqualTo(
        SegmentMetadata.BYTES + entry0.getBytes().length);
  }

  @Test
  public void createSegmentIndex() throws Exception {
    long entryOffset0 = SegmentMetadata.BYTES;
    long entryOffset1 = entryOffset0 + entry0.getBytes().length;
    ImmutableSortedMap<String, Long> keyOffsetMap = ImmutableSortedMap.<String, Long>naturalOrder()
        .put("key0", entryOffset0).put("key1", entryOffset1).build();

    SegmentIndex segmentIndex;

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try (MockedStatic<Files> fileMockedStatic = mockStatic(Files.class)) {
      fileMockedStatic.when(() -> Files.newOutputStream(any(), any())).thenReturn(outputStream);

      segmentIndex = factory.createSegmentIndex(keyOffsetMap, segmentNumber);
    }

    assertThat(outputStream.toByteArray()).isEqualTo(Bytes.concat(
        new SegmentIndexMetadata(segmentNumber).getBytes(),
        new SegmentIndexEntry("key0", entryOffset0).getBytes(),
        new SegmentIndexEntry("key1", entryOffset1).getBytes()));

    assertThat(segmentIndex.getSegmentNumber()).isEqualTo(segmentNumber.value());
    assertThat(segmentIndex.getKeyOffset("key0")).hasValue(entryOffset0);
    assertThat(segmentIndex.getKeyOffset("key1")).hasValue(entryOffset1);
  }
}
