package dev.sbutler.bitflask.storage.segment;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.google.common.primitives.Bytes;
import com.google.common.util.concurrent.testing.TestingExecutors;
import dev.sbutler.bitflask.common.primitives.UnsignedShort;
import dev.sbutler.bitflask.storage.entry.Entry;
import dev.sbutler.bitflask.storage.entry.EntryMetadata;
import dev.sbutler.bitflask.storage.segment.Segment.Factory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

@SuppressWarnings({"UnstableApiUsage", "resource"})
public class SegmentTest {

  private final UnsignedShort zeroUnsignedShort = UnsignedShort.valueOf(0);

  private final SegmentMetadata metadata = new SegmentMetadata(zeroUnsignedShort,
      UnsignedShort.valueOf(1));
  private final BloomFilter<String> keyFilter =
      BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), 1);
  SegmentIndex emptySegmentIndex = new SegmentIndexDense(
      new SegmentIndexMetadata(zeroUnsignedShort),
      ImmutableSortedMap.of());
  Path filePath = Paths.get("src/test/resources/segment0" + Segment.FILE_EXTENSION);

  private final Factory factory = new Factory(TestingExecutors.sameThreadScheduledExecutor());

  @Test
  public void construction_mismatchSegmentNumber_throwsIllegalArgumentException() {
    SegmentIndex segmentIndex = new SegmentIndexDense(
        new SegmentIndexMetadata(UnsignedShort.valueOf(1)),
        ImmutableSortedMap.of());

    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class,
            () -> factory.create(metadata, keyFilter, segmentIndex, filePath));

    assertThat(e).hasMessageThat().ignoringCase()
        .contains("SegmentMetadata segmentNumber does not match SegmentIndex segmentNumber.");
  }

  @Test
  public void getSegmentNumber_matchesSegmentMetadata() {
    Segment segment = factory.create(metadata, keyFilter, emptySegmentIndex, filePath);

    assertThat(segment.getSegmentNumber()).isEqualTo(metadata.getSegmentNumber());
  }

  @Test
  public void getSegmentLevel_matchesSegmentMetadata() {
    Segment segment = factory.create(metadata, keyFilter, emptySegmentIndex, filePath);

    assertThat(segment.getSegmentLevel()).isEqualTo(metadata.getSegmentLevel());
  }

  @Test
  public void mightContain_absent_returnsFalse() {
    String key = "key";

    Segment segment = factory.create(metadata, keyFilter, emptySegmentIndex, filePath);

    assertThat(segment.mightContain(key)).isFalse();
  }

  @Test
  public void mightContain_presentInBloomFilter_returnsTrue() {
    String key = "key";
    keyFilter.put(key);

    Segment segment = factory.create(metadata, keyFilter, emptySegmentIndex, filePath);

    assertThat(segment.mightContain(key)).isTrue();
  }

  @Test
  public void mightContain_presentInSegmentIndex_returnsTrue() {
    String key = "key";
    SegmentIndex segmentIndex = new SegmentIndexDense(
        new SegmentIndexMetadata(zeroUnsignedShort),
        ImmutableSortedMap.of(key, 0L));

    Segment segment = factory.create(metadata, keyFilter, segmentIndex, filePath);

    assertThat(segment.mightContain(key)).isTrue();
  }

  @Test
  public void readEntry_found() throws Exception {
    String key = "key";
    String value = "value";
    keyFilter.put(key);
    SegmentIndex segmentIndex = new SegmentIndexDense(
        new SegmentIndexMetadata(UnsignedShort.valueOf(0)),
        ImmutableSortedMap.of(key, 0L));
    Segment segment = factory.create(metadata, keyFilter, segmentIndex, filePath);

    Entry storedEntry = new Entry(Instant.now().getEpochSecond(), key, value);
    InputStream is = new ByteArrayInputStream(storedEntry.getBytes());

    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      filesMockedStatic.when(() -> Files.newInputStream(any(), any())).thenReturn(is);
      Optional<Entry> entry = segment.readEntry(key).get();
      assertThat(entry).hasValue(storedEntry);
    }
  }

  @Test
  public void readEntry_found_skipToOffset() throws Exception {
    String key0 = "key0", key1 = "key1";
    String value0 = "value0", value1 = "value1";
    keyFilter.put(key0);
    keyFilter.put(key1);

    Entry storedEntry0 = new Entry(Instant.now().getEpochSecond(), key0, value0);
    Entry storedEntry1 = new Entry(Instant.now().getEpochSecond(), key1, value1);

    SegmentIndex segmentIndex = new SegmentIndexDense(
        new SegmentIndexMetadata(UnsignedShort.valueOf(0)),
        ImmutableSortedMap.of(key0, 0L, key1, (long) storedEntry1.getBytes().length));
    Segment segment = factory.create(metadata, keyFilter, segmentIndex, filePath);

    InputStream is = new ByteArrayInputStream(Bytes.concat(
        storedEntry0.getBytes(),
        storedEntry1.getBytes()));

    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      filesMockedStatic.when(() -> Files.newInputStream(any(), any())).thenReturn(is);
      Optional<Entry> entry = segment.readEntry(key1).get();
      assertThat(entry).hasValue(storedEntry1);
    }
  }

  @Test
  public void readEntry_found_numberReadMismatch_key() {
    String key = "key";
    String value = "value";
    keyFilter.put(key);
    SegmentIndex segmentIndex = new SegmentIndexDense(
        new SegmentIndexMetadata(UnsignedShort.valueOf(0)),
        ImmutableSortedMap.of(key, 0L));
    Segment segment = factory.create(metadata, keyFilter, segmentIndex, filePath);

    EntryMetadata storedMetadata = new EntryMetadata(
        Instant.now().getEpochSecond(),
        UnsignedShort.valueOf(key.length()),
        UnsignedShort.valueOf(value.length()));
    InputStream is = new ByteArrayInputStream(storedMetadata.getBytes());

    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      filesMockedStatic.when(() -> Files.newInputStream(any(), any())).thenReturn(is);
      ExecutionException e =
          assertThrows(ExecutionException.class, () -> segment.readEntry(key).get());

      assertThat(e).hasCauseThat().hasMessageThat().ignoringCase()
          .contains("Read key length did not match entry.");
    }
  }

  @Test
  public void readEntry_found_numberReadMismatch_value() {
    String key = "key";
    String value = "value";
    keyFilter.put(key);
    SegmentIndex segmentIndex = new SegmentIndexDense(
        new SegmentIndexMetadata(UnsignedShort.valueOf(0)),
        ImmutableSortedMap.of(key, 0L));
    Segment segment = factory.create(metadata, keyFilter, segmentIndex, filePath);

    EntryMetadata storedMetadata = new EntryMetadata(
        Instant.now().getEpochSecond(),
        UnsignedShort.valueOf(key.length()),
        UnsignedShort.valueOf(value.length()));
    InputStream is = new ByteArrayInputStream(Bytes.concat(
        storedMetadata.getBytes(),
        key.getBytes(StandardCharsets.UTF_8)));

    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      filesMockedStatic.when(() -> Files.newInputStream(any(), any())).thenReturn(is);
      ExecutionException e =
          assertThrows(ExecutionException.class, () -> segment.readEntry(key).get());

      assertThat(e).hasCauseThat().hasMessageThat().ignoringCase()
          .contains("Read value length did not match entry.");
    }
  }

  @Test
  public void readEntry_emptyFile() throws Exception {
    String key = "key";
    keyFilter.put(key);
    SegmentIndex segmentIndex = new SegmentIndexDense(
        new SegmentIndexMetadata(UnsignedShort.valueOf(0)),
        ImmutableSortedMap.of(key, 0L));
    Segment segment = factory.create(metadata, keyFilter, segmentIndex, filePath);

    InputStream is = new ByteArrayInputStream(new byte[0]);

    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      filesMockedStatic.when(() -> Files.newInputStream(any(), any())).thenReturn(is);
      Optional<Entry> entry = segment.readEntry(key).get();
      assertThat(entry).isEmpty();
    }
  }
}
