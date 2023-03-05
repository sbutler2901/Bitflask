package dev.sbutler.bitflask.storage.lsm.segment;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.google.common.primitives.Bytes;
import dev.sbutler.bitflask.common.primitives.UnsignedShort;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
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

@SuppressWarnings({"UnstableApiUsage"})
public class SegmentFactoryTest {

  private final Path TEST_RESOURCE_PATH = Paths.get("src/test/resources/");

  private final Entry ENTRY_0 = new Entry(Instant.now().getEpochSecond(), "key0", "value0");
  private final Entry ENTRY_1 = new Entry(Instant.now().getEpochSecond(), "key1", "value1");

  private final UnsignedShort SEGMENT_NUMBER = UnsignedShort.valueOf(0);
  private final UnsignedShort SEGMENT_LEVEL = UnsignedShort.valueOf(0);

  private final StorageConfigurations config = mock(StorageConfigurations.class);
  private final SegmentIndexFactory indexFactory = mock(SegmentIndexFactory.class);
  private final SegmentIndex segmentIndex = mock(SegmentIndex.class);

  private SegmentFactory factory;

  @BeforeEach
  public void beforeEach() throws Exception {
    when(indexFactory.create(any(), any())).thenReturn(segmentIndex);
    when(segmentIndex.getSegmentNumber()).thenReturn(SEGMENT_NUMBER.value());
    when(config.getStorageStoreDirectoryPath()).thenReturn(TEST_RESOURCE_PATH);

    factory = new SegmentFactory.Factory(config, indexFactory).create(SEGMENT_NUMBER.value());
  }

  @Test
  public void create() throws Exception {
    ImmutableSortedMap<String, Entry> keyEntryMap = ImmutableSortedMap.<String, Entry>naturalOrder()
        .put(ENTRY_0.key(), ENTRY_0)
        .build();

    Segment segment;

    ByteArrayOutputStream segmentOutputStream = new ByteArrayOutputStream();
    try (MockedStatic<Files> fileMockedStatic = mockStatic(Files.class)) {
      fileMockedStatic.when(() -> Files.newOutputStream(
              Path.of(TEST_RESOURCE_PATH.toString(), "segment_0.seg"), StandardOpenOption.CREATE_NEW))
          .thenReturn(segmentOutputStream);

      segment = factory.create(keyEntryMap);
    }

    assertThat(segment.getSegmentNumber()).isEqualTo(SEGMENT_NUMBER.value());
    assertThat(segment.getSegmentLevel()).isEqualTo(SEGMENT_LEVEL.value());
    assertThat(segment.mightContain(ENTRY_0.key())).isTrue();

    assertThat(segmentOutputStream.toByteArray()).isEqualTo(Bytes.concat(
        new SegmentMetadata(SEGMENT_NUMBER, SEGMENT_LEVEL).getBytes(),
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
        () -> factory.create(keyEntryMap));

    assertThat(e).hasMessageThat().ignoringCase().isEqualTo("keyEntryMap is empty.");
  }

  @Test
  public void writeSegment() throws Exception {
    ImmutableSortedMap<String, Entry> keyEntryMap = ImmutableSortedMap.<String, Entry>naturalOrder()
        .put(ENTRY_0.key(), ENTRY_0)
        .put(ENTRY_1.key(), ENTRY_1)
        .build();
    SegmentMetadata metadata = new SegmentMetadata(SEGMENT_NUMBER, SEGMENT_LEVEL);
    BloomFilter<String> keyFilter = BloomFilter.create(Funnels.stringFunnel(
        StandardCharsets.UTF_8), keyEntryMap.size());

    ImmutableSortedMap<String, Long> keyOffsetMap;

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try (MockedStatic<Files> fileMockedStatic = mockStatic(Files.class)) {
      fileMockedStatic.when(() -> Files.newOutputStream(any(), any())).thenReturn(outputStream);

      keyOffsetMap = factory.writeSegment(keyEntryMap, metadata, keyFilter, TEST_RESOURCE_PATH);
    }

    assertThat(keyFilter.mightContain(ENTRY_0.key())).isTrue();
    assertThat(keyFilter.mightContain(ENTRY_1.key())).isTrue();

    assertThat(outputStream.toByteArray()).isEqualTo(Bytes.concat(
        metadata.getBytes(),
        ENTRY_0.getBytes(),
        ENTRY_1.getBytes()));

    assertThat(keyOffsetMap.get(ENTRY_0.key())).isEqualTo(SegmentMetadata.BYTES);
    assertThat(keyOffsetMap.get(ENTRY_1.key())).isEqualTo(
        SegmentMetadata.BYTES + ENTRY_0.getBytes().length);
  }
}
