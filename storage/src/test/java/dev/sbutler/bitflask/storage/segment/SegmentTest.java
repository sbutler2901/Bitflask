package dev.sbutler.bitflask.storage.segment;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import dev.sbutler.bitflask.common.primitives.UnsignedShort;
import dev.sbutler.bitflask.storage.entry.Entry;
import dev.sbutler.bitflask.storage.entry.EntryReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"UnstableApiUsage"})
public class SegmentTest {

  private final UnsignedShort zeroUnsignedShort = UnsignedShort.valueOf(0);

  private final SegmentMetadata metadata = new SegmentMetadata(zeroUnsignedShort,
      UnsignedShort.valueOf(1));
  private final EntryReader entryReader = mock(EntryReader.class);
  private final BloomFilter<String> keyFilter =
      BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), 1);
  SegmentIndex emptySegmentIndex = new SegmentIndexDense(
      new SegmentIndexMetadata(zeroUnsignedShort),
      ImmutableSortedMap.of());

  @Test
  public void construction_mismatchSegmentNumber_throwsIllegalArgumentException() {
    SegmentIndex segmentIndex = new SegmentIndexDense(
        new SegmentIndexMetadata(UnsignedShort.valueOf(1)),
        ImmutableSortedMap.of());

    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class,
            () -> Segment.create(metadata, entryReader, keyFilter, segmentIndex));

    assertThat(e).hasMessageThat().ignoringCase()
        .contains("SegmentMetadata segmentNumber does not match SegmentIndex segmentNumber.");
  }

  @Test
  public void getSegmentNumber_matchesSegmentMetadata() {
    Segment segment = Segment.create(metadata, entryReader, keyFilter, emptySegmentIndex);

    assertThat(segment.getSegmentNumber()).isEqualTo(metadata.getSegmentNumber());
  }

  @Test
  public void getSegmentLevel_matchesSegmentMetadata() {
    Segment segment = Segment.create(metadata, entryReader, keyFilter, emptySegmentIndex);

    assertThat(segment.getSegmentLevel()).isEqualTo(metadata.getSegmentLevel());
  }

  @Test
  public void mightContain_absent_returnsFalse() {
    String key = "key";

    Segment segment = Segment.create(metadata, entryReader, keyFilter, emptySegmentIndex);

    assertThat(segment.mightContain(key)).isFalse();
  }

  @Test
  public void mightContain_presentInBloomFilter_returnsTrue() {
    String key = "key";
    keyFilter.put(key);

    Segment segment = Segment.create(metadata, entryReader, keyFilter, emptySegmentIndex);

    assertThat(segment.mightContain(key)).isTrue();
  }

  @Test
  public void mightContain_presentInSegmentIndex_returnsTrue() {
    String key = "key";
    SegmentIndex segmentIndex = new SegmentIndexDense(
        new SegmentIndexMetadata(zeroUnsignedShort),
        ImmutableSortedMap.of(key, 0L));

    Segment segment = Segment.create(metadata, entryReader, keyFilter, segmentIndex);

    assertThat(segment.mightContain(key)).isTrue();
  }

  @Test
  public void readEntry_notFound() throws Exception {
    SegmentIndex segmentIndex = new SegmentIndexDense(
        new SegmentIndexMetadata(UnsignedShort.valueOf(0)),
        ImmutableSortedMap.of());
    Segment segment = Segment.create(metadata, entryReader, keyFilter, segmentIndex);

    Optional<Entry> readEntry = segment.readEntry("key").get();

    assertThat(readEntry).isEmpty();
  }

  @Test
  public void readEntry_found() throws Exception {
    String key = "key";
    String value = "value";
    keyFilter.put(key);
    SegmentIndex segmentIndex = new SegmentIndexDense(
        new SegmentIndexMetadata(UnsignedShort.valueOf(0)),
        ImmutableSortedMap.of(key, 0L));
    Entry entry = new Entry(Instant.now().getEpochSecond(), key, value);
    Segment segment = Segment.create(metadata, entryReader, keyFilter, segmentIndex);
    when(entryReader.findEntryFromOffset(anyString(), anyLong()))
        .thenReturn(immediateFuture(Optional.of(entry)));

    Optional<Entry> readEntry = segment.readEntry(key).get();

    assertThat(readEntry).hasValue(entry);
  }

  @Test
  public void readEntry_offsetNotFound() throws Exception {
    String key = "key";
    keyFilter.put(key);
    Segment segment = Segment.create(metadata, entryReader, keyFilter, emptySegmentIndex);

    Optional<Entry> readEntry = segment.readEntry(key).get();

    assertThat(readEntry).isEmpty();
    verify(entryReader, times(0)).findEntryFromOffset(anyString(), anyLong());
  }
}
