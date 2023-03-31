package dev.sbutler.bitflask.storage.lsm.segment;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import com.google.common.collect.ImmutableSortedMap;
import dev.sbutler.bitflask.common.primitives.UnsignedShort;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class SegmentIndexDenseTest {

  private static final Path PATH = Path.of("/tmp/index_0.idx");

  @Test
  public void mightContain_absent_false() {
    SegmentIndexMetadata metadata = new SegmentIndexMetadata(UnsignedShort.valueOf(0));
    ImmutableSortedMap<String, Long> keyOffsetMap =
        ImmutableSortedMap.<String, Long>naturalOrder().buildOrThrow();
    SegmentIndexDense segmentIndex = new SegmentIndexDense(PATH, metadata, keyOffsetMap);

    assertThat(segmentIndex.mightContain("key")).isFalse();
  }

  @Test
  public void mightContain_present_true() {
    SegmentIndexMetadata metadata = new SegmentIndexMetadata(UnsignedShort.valueOf(0));
    ImmutableSortedMap<String, Long> keyOffsetMap =
        ImmutableSortedMap.<String, Long>naturalOrder()
            .put("key", 0L).buildOrThrow();
    SegmentIndexDense segmentIndex = new SegmentIndexDense(PATH, metadata, keyOffsetMap);

    assertThat(segmentIndex.mightContain("key")).isTrue();
  }

  @Test
  public void getKeyOffset_absent() {
    SegmentIndexMetadata metadata = new SegmentIndexMetadata(UnsignedShort.valueOf(0));
    ImmutableSortedMap<String, Long> keyOffsetMap =
        ImmutableSortedMap.<String, Long>naturalOrder().buildOrThrow();
    SegmentIndexDense segmentIndex = new SegmentIndexDense(PATH, metadata, keyOffsetMap);

    assertThat(segmentIndex.getKeyOffset("key")).isEmpty();
  }

  @Test
  public void getKeyOffset_present() {
    SegmentIndexMetadata metadata = new SegmentIndexMetadata(UnsignedShort.valueOf(0));
    ImmutableSortedMap<String, Long> keyOffsetMap =
        ImmutableSortedMap.<String, Long>naturalOrder()
            .put("key", 0L).buildOrThrow();
    SegmentIndexDense segmentIndex = new SegmentIndexDense(PATH, metadata, keyOffsetMap);

    assertThat(segmentIndex.getKeyOffset("key")).hasValue(0L);
  }

  @Test
  public void getSegmentNumber() {
    SegmentIndexMetadata metadata = new SegmentIndexMetadata(UnsignedShort.valueOf(0));
    ImmutableSortedMap<String, Long> keyOffsetMap =
        ImmutableSortedMap.<String, Long>naturalOrder().buildOrThrow();
    SegmentIndexDense segmentIndex = new SegmentIndexDense(PATH, metadata, keyOffsetMap);

    assertThat(segmentIndex.getSegmentNumber()).isEqualTo(0);
  }
}
