package dev.sbutler.bitflask.storage.segment;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import dev.sbutler.bitflask.common.primitives.UnsignedShort;
import dev.sbutler.bitflask.storage.entry.Entry;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@SuppressWarnings("UnstableApiUsage")
public class SegmentTest {

  private final UnsignedShort zeroUnsignedShort = UnsignedShort.valueOf(0);

  private final SegmentMetadata metadata = new SegmentMetadata(zeroUnsignedShort,
      UnsignedShort.valueOf(1));
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
            () -> new Segment(metadata, keyFilter, segmentIndex));

    assertThat(e).hasMessageThat().ignoringCase()
        .contains("SegmentMetadata segmentNumber does not match SegmentIndex segmentNumber.");
  }

  @Test
  public void getSegmentNumber_matchesSegmentMetadata() {
    Segment segment = new Segment(metadata, keyFilter, emptySegmentIndex);

    assertThat(segment.getSegmentNumber()).isEqualTo(metadata.getSegmentNumber());
  }

  @Test
  public void getSegmentLevel_matchesSegmentMetadata() {
    Segment segment = new Segment(metadata, keyFilter, emptySegmentIndex);

    assertThat(segment.getSegmentLevel()).isEqualTo(metadata.getSegmentLevel());
  }

  @Test
  public void mightContain_absent_returnsFalse() {
    String key = "key";

    Segment segment = new Segment(metadata, keyFilter, emptySegmentIndex);

    assertThat(segment.mightContain(key)).isFalse();
  }

  @Test
  public void mightContain_presentInBloomFilter_returnsTrue() {
    String key = "key";
    keyFilter.put(key);

    Segment segment = new Segment(metadata, keyFilter, emptySegmentIndex);

    assertThat(segment.mightContain(key)).isTrue();
  }

  @Test
  public void mightContain_presentInSegmentIndex_returnsTrue() {
    String key = "key";
    SegmentIndex segmentIndex = new SegmentIndexDense(
        new SegmentIndexMetadata(zeroUnsignedShort),
        ImmutableSortedMap.of(key, 0L));

    Segment segment = new Segment(metadata, keyFilter, segmentIndex);

    assertThat(segment.mightContain(key)).isTrue();
  }

  @Test
  public void readEntry() throws Exception {
    String key = "key";
    Segment segment = new Segment(metadata, keyFilter, emptySegmentIndex);

    Optional<Entry> entry = segment.readEntry(key).get();

    assertThat(entry).isEmpty();
  }
}
