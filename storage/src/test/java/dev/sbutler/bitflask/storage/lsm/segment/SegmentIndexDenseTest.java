package dev.sbutler.bitflask.storage.lsm.segment;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import com.google.common.collect.ImmutableSortedMap;
import dev.sbutler.bitflask.common.primitives.UnsignedShort;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class SegmentIndexDenseTest {

  private static final Path PATH = Path.of("/tmp/index_0.idx");
  private static final SegmentIndexMetadata METADATA =
      new SegmentIndexMetadata(UnsignedShort.valueOf(0));

  private static final ImmutableSortedMap<String, Long> KEY_OFFSET_MAP =
      ImmutableSortedMap.<String, Long>naturalOrder()
          .put("key", 0L).buildOrThrow();
  private static final ImmutableSortedMap<String, Long> KEY_OFFSET_MAP_EMPTY =
      ImmutableSortedMap.<String, Long>naturalOrder().buildOrThrow();

  @Test
  public void mightContain_absent_false() {
    SegmentIndexDense segmentIndex = new SegmentIndexDense(PATH, METADATA, KEY_OFFSET_MAP_EMPTY);
    assertThat(segmentIndex.mightContain("key")).isFalse();
  }

  @Test
  public void mightContain_present_true() {
    SegmentIndexDense segmentIndex = new SegmentIndexDense(PATH, METADATA, KEY_OFFSET_MAP);
    assertThat(segmentIndex.mightContain("key")).isTrue();
  }

  @Test
  public void getKeyOffset_absent() {
    SegmentIndexDense segmentIndex = new SegmentIndexDense(PATH, METADATA, KEY_OFFSET_MAP_EMPTY);
    assertThat(segmentIndex.getKeyOffset("key")).isEmpty();
  }

  @Test
  public void getKeyOffset_present() {
    SegmentIndexDense segmentIndex = new SegmentIndexDense(PATH, METADATA, KEY_OFFSET_MAP);
    assertThat(segmentIndex.getKeyOffset("key")).hasValue(0L);
  }

  @Test
  public void getSegmentNumber() {
    SegmentIndexDense segmentIndex = new SegmentIndexDense(PATH, METADATA, KEY_OFFSET_MAP);
    assertThat(segmentIndex.getSegmentNumber()).isEqualTo(0);
  }

  @Test
  public void getFilePath() {
    SegmentIndexDense segmentIndex = new SegmentIndexDense(PATH, METADATA, KEY_OFFSET_MAP);
    assertThat(segmentIndex.getFilePath()).isEqualTo(PATH);
  }
}
