package dev.sbutler.bitflask.storage.lsm.segment;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableListMultimap;
import org.junit.jupiter.api.Test;

public class SegmentLevelMultiMapTest {

  @Test
  public void empty() {
    SegmentLevelMultiMap segmentLevelMultiMap = SegmentLevelMultiMap.create();

    assertThat(segmentLevelMultiMap.getSegmentLevels()).isEmpty();
    assertThat(segmentLevelMultiMap.getSegmentsInLevel(0)).isEmpty();
  }

  @Test
  public void getSegmentLevels_ascendingOrder() {
    SegmentLevelMultiMap segmentLevelMultiMap = SegmentLevelMultiMap.create(
        ImmutableListMultimap.of(1, mock(Segment.class), 0, mock(Segment.class)));

    assertThat(segmentLevelMultiMap.getSegmentLevels()).isInOrder();
  }

  @Test
  public void getSegmentsInLevel() {
    Segment segment0 = mock(Segment.class);
    Segment segment1 = mock(Segment.class);
    SegmentLevelMultiMap segmentLevelMultiMap = SegmentLevelMultiMap.create(
        ImmutableListMultimap.of(1, segment1, 0, segment0));

    assertThat(segmentLevelMultiMap.getSegmentsInLevel(0)).containsExactly(segment0);
    assertThat(segmentLevelMultiMap.getSegmentsInLevel(1)).containsExactly(segment1);
  }
}
