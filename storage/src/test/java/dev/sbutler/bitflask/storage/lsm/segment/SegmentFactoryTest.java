package dev.sbutler.bitflask.storage.lsm.segment;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.google.common.primitives.Bytes;
import dev.sbutler.bitflask.common.primitives.UnsignedShort;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.exceptions.StorageLoadException;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import dev.sbutler.bitflask.storage.lsm.entry.EntryReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.SortedMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

@SuppressWarnings({"resource", "UnstableApiUsage", "ResultOfMethodCallIgnored"})
public class SegmentFactoryTest {

  private final Path SEGMENT_PATH = Path.of("/tmp/segment_0.seg");
  private final Path TEST_RESOURCE_PATH = Paths.get("src/test/resources/");

  private final Entry ENTRY_0 = new Entry(Instant.now().getEpochSecond(), "key0", "value0");
  private final Entry ENTRY_1 = new Entry(Instant.now().getEpochSecond(), "key1", "value1");

  private final UnsignedShort SEGMENT_NUMBER = UnsignedShort.valueOf(0);
  private final UnsignedShort SEGMENT_LEVEL = UnsignedShort.valueOf(0);

  private final SegmentMetadata METADATA = new SegmentMetadata(SEGMENT_NUMBER, SEGMENT_LEVEL);

  private final StorageConfigurations config = mock(StorageConfigurations.class);
  private final SegmentIndexFactory indexFactory = mock(SegmentIndexFactory.class);
  private final SegmentIndex segmentIndex = mock(SegmentIndex.class);

  private final SegmentFactory factory = new SegmentFactory(config, indexFactory);

  @BeforeEach
  public void beforeEach() throws Exception {
    when(indexFactory.create(any(), any())).thenReturn(segmentIndex);
    when(segmentIndex.getSegmentNumber()).thenReturn(SEGMENT_NUMBER.value());
    when(config.getStoreDirectoryPath()).thenReturn(TEST_RESOURCE_PATH);
  }

  @Test
  public void create() throws Exception {
    ImmutableSortedMap<String, Entry> keyEntryMap = ImmutableSortedMap.<String, Entry>naturalOrder()
        .put(ENTRY_0.key(), ENTRY_0)
        .build();

    Segment segment;

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try (MockedStatic<Files> fileMockedStatic = mockStatic(Files.class)) {
      fileMockedStatic.when(() -> Files.newOutputStream(any(), any())).thenReturn(outputStream);

      segment = factory.create(keyEntryMap, 0);
    }

    assertThat(segment.getSegmentNumber()).isEqualTo(SEGMENT_NUMBER.value());
    assertThat(segment.getSegmentLevel()).isEqualTo(SEGMENT_LEVEL.value());
    assertThat(segment.mightContain(ENTRY_0.key())).isTrue();
    assertThat(segment.getNumBytesSize()).isEqualTo(ENTRY_0.getNumBytesSize());

    assertThat(outputStream.toByteArray()).isEqualTo(Bytes.concat(
        METADATA.getBytes(),
        ENTRY_0.getBytes()));

    verify(indexFactory, times(1)).create(
        ImmutableSortedMap.<String, Long>naturalOrder()
            .put(ENTRY_0.key(), (long) SegmentMetadata.BYTES)
            .build(),
        SEGMENT_NUMBER);
  }

  @Test
  public void create_emptyKeyEntryMap() {
    ImmutableSortedMap<String, Entry> keyEntryMap = ImmutableSortedMap.<String, Entry>naturalOrder()
        .build();

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> factory.create(keyEntryMap, 0));

    assertThat(e).hasMessageThat().ignoringCase().isEqualTo("keyEntryMap is empty.");
  }

  @Test
  public void writeSegment() throws Exception {
    SortedMap<String, Entry> keyEntryMap = ImmutableSortedMap.<String, Entry>naturalOrder()
        .put(ENTRY_0.key(), ENTRY_0)
        .put(ENTRY_1.key(), ENTRY_1)
        .build();
    BloomFilter<String> keyFilter = BloomFilter.create(Funnels.stringFunnel(
        StandardCharsets.UTF_8), keyEntryMap.size());

    SortedMap<String, Long> keyOffsetMap;

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try (MockedStatic<Files> fileMockedStatic = mockStatic(Files.class)) {
      fileMockedStatic.when(() -> Files.newOutputStream(any(), any())).thenReturn(outputStream);

      keyOffsetMap = factory.writeSegment(keyEntryMap, METADATA, keyFilter, TEST_RESOURCE_PATH);
    }

    assertThat(keyFilter.mightContain(ENTRY_0.key())).isTrue();
    assertThat(keyFilter.mightContain(ENTRY_1.key())).isTrue();

    assertThat(outputStream.toByteArray()).isEqualTo(Bytes.concat(
        METADATA.getBytes(),
        ENTRY_0.getBytes(),
        ENTRY_1.getBytes()));

    assertThat(keyOffsetMap.get(ENTRY_0.key())).isEqualTo(SegmentMetadata.BYTES);
    assertThat(keyOffsetMap.get(ENTRY_1.key())).isEqualTo(
        SegmentMetadata.BYTES + ENTRY_0.getBytes().length);
  }

  @Test
  public void loadFromPath_success() throws Exception {
    ImmutableMap<Integer, SegmentIndex> segmentNumberToIndexMap =
        ImmutableMap.of(METADATA.getSegmentNumber(), segmentIndex);
    ByteArrayInputStream inputStream = new ByteArrayInputStream(METADATA.getBytes());
    EntryReader entryReader = mock(EntryReader.class);
    when(entryReader.readAllEntriesFromOffset(SegmentMetadata.BYTES))
        .thenReturn(ImmutableList.of(ENTRY_0, ENTRY_1));

    Segment segment;
    try (MockedStatic<Files> fileMockedStatic = mockStatic(Files.class);
        MockedStatic<EntryReader> entryReaderMockedStatic = mockStatic(EntryReader.class)) {
      fileMockedStatic.when(() -> Files.newInputStream(any())).thenReturn(inputStream);
      entryReaderMockedStatic.when(() -> EntryReader.create(any()))
          .thenReturn(entryReader);

      segment = factory.loadFromPath(SEGMENT_PATH, segmentNumberToIndexMap);
    }

    assertThat(segment.getSegmentNumber()).isEqualTo(METADATA.getSegmentNumber());
    assertThat(segment.getSegmentLevel()).isEqualTo(METADATA.getSegmentLevel());

    assertThat(segment.mightContain(ENTRY_0.key())).isTrue();
    assertThat(segment.mightContain(ENTRY_1.key())).isTrue();
  }

  @Test
  public void loadFromPath_emptyFile_throwsStorageLoadException() {
    ImmutableMap<Integer, SegmentIndex> segmentNumberToIndexMap = ImmutableMap.of();
    ByteArrayInputStream is = new ByteArrayInputStream(new byte[]{});

    StorageLoadException e;
    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      filesMockedStatic.when(() -> Files.newInputStream(any())).thenReturn(is);

      e = assertThrows(StorageLoadException.class,
          () -> factory.loadFromPath(SEGMENT_PATH, segmentNumberToIndexMap));
    }

    assertThat(e).hasMessageThat().isEqualTo(String.format(
        "SegmentMetadata bytes read too short. Expected [%d], actual [%d]",
        SegmentMetadata.BYTES, 0));
  }

  @Test
  public void loadFromPath_matchingSegmentIndexNotFound_throwsStorageLoadException()
      throws Exception {
    ImmutableMap<Integer, SegmentIndex> segmentNumberToIndexMap = ImmutableMap.of();
    ByteArrayInputStream inputStream = new ByteArrayInputStream(METADATA.getBytes());
    EntryReader entryReader = mock(EntryReader.class);
    when(entryReader.readAllEntriesFromOffset(SegmentMetadata.BYTES))
        .thenReturn(ImmutableList.of(ENTRY_0, ENTRY_1));

    StorageLoadException e;
    try (MockedStatic<Files> fileMockedStatic = mockStatic(Files.class);
        MockedStatic<EntryReader> entryReaderMockedStatic = mockStatic(EntryReader.class)) {
      fileMockedStatic.when(() -> Files.newInputStream(any())).thenReturn(inputStream);
      entryReaderMockedStatic.when(() -> EntryReader.create(any())).thenReturn(entryReader);

      e = assertThrows(StorageLoadException.class,
          () -> factory.loadFromPath(SEGMENT_PATH, segmentNumberToIndexMap));
    }

    assertThat(e).hasMessageThat().isEqualTo(String.format(
        "Could not find a SegmentIndex with expected segment number [%d]",
        METADATA.getSegmentNumber()));
  }
}
