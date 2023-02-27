package dev.sbutler.bitflask.storage.segment;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.google.common.util.concurrent.testing.TestingExecutors;
import dev.sbutler.bitflask.common.primitives.UnsignedShort;
import dev.sbutler.bitflask.storage.entry.Entry;
import dev.sbutler.bitflask.storage.segment.Segment.Factory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
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
  Path filePath = Path.of("segment0" + Segment.FILE_EXTENSION);

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
  public void readEntry() throws Exception {
    String key = "key";
    Segment segment = factory.create(metadata, keyFilter, emptySegmentIndex, filePath);

    Optional<Entry> entry = segment.readEntry(key).get();

    assertThat(entry).isEmpty();
  }
}
