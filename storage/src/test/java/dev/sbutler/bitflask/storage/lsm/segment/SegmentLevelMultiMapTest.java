package dev.sbutler.bitflask.storage.lsm.segment;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SegmentLevelMultiMapTest {

  private final Segment SEGMENT_0 = mock(Segment.class);
  private final Segment SEGMENT_1 = mock(Segment.class);

  @BeforeEach
  public void beforeEach() {
    when(SEGMENT_0.getSegmentLevel()).thenReturn(0);
    when(SEGMENT_1.getSegmentLevel()).thenReturn(1);
  }

  @Test
  public void empty() {
    SegmentLevelMultiMap segmentLevelMultiMap = new SegmentLevelMultiMap.Builder(
        ImmutableListMultimap.of()).build();

    assertThat(segmentLevelMultiMap.getSegmentLevels()).isEmpty();
    assertThat(segmentLevelMultiMap.getSegmentsInLevel(0)).isEmpty();
  }

  @Test
  public void getSegmentLevels_ascendingOrder() {
    SegmentLevelMultiMap segmentLevelMultiMap = new SegmentLevelMultiMap.Builder(
        ImmutableListMultimap.of(1, SEGMENT_1, 0, SEGMENT_0)).build();

    assertThat(segmentLevelMultiMap.getSegmentLevels()).isInOrder();
  }

  @Test
  public void getSegmentsInLevel() {
    SegmentLevelMultiMap segmentLevelMultiMap = new SegmentLevelMultiMap.Builder(
        ImmutableListMultimap.of(1, SEGMENT_1, 0, SEGMENT_0)).build();

    assertThat(segmentLevelMultiMap.getSegmentsInLevel(0)).containsExactly(SEGMENT_0);
    assertThat(segmentLevelMultiMap.getSegmentsInLevel(1)).containsExactly(SEGMENT_1);
  }

  @Test
  public void toBuilder() {
    SegmentLevelMultiMap segmentLevelMultiMap = new SegmentLevelMultiMap.Builder(
        ImmutableListMultimap.of(1, SEGMENT_1, 0, SEGMENT_0)).build();

    SegmentLevelMultiMap newSegmentLevelMultiMap = segmentLevelMultiMap.toBuilder().build();

    assertThat(newSegmentLevelMultiMap.getSegmentLevels()).isEqualTo(
        segmentLevelMultiMap.getSegmentLevels());
    assertThat(newSegmentLevelMultiMap.getSegmentsInLevel(0)).isEqualTo(
        segmentLevelMultiMap.getSegmentsInLevel(0));
    assertThat(newSegmentLevelMultiMap.getSegmentsInLevel(1)).isEqualTo(
        segmentLevelMultiMap.getSegmentsInLevel(1));
  }

  @Test
  public void builder_fromMultiMap() {
    SegmentLevelMultiMap newSegmentLevelMultiMap = new SegmentLevelMultiMap.Builder(
        ImmutableListMultimap.of(1, SEGMENT_1, 0, SEGMENT_0)).build();

    assertThat(newSegmentLevelMultiMap.getSegmentLevels()).isEqualTo(
        ImmutableSet.of(0, 1));
    assertThat(newSegmentLevelMultiMap.getSegmentsInLevel(0)).isEqualTo(
        ImmutableList.of(SEGMENT_0));
    assertThat(newSegmentLevelMultiMap.getSegmentsInLevel(1)).isEqualTo(
        ImmutableList.of(SEGMENT_1));
  }

  @Test
  public void builder_add() {
    SegmentLevelMultiMap segmentLevelMultiMap = SegmentLevelMultiMap.builder()
        .add(SEGMENT_1)
        .add(SEGMENT_0)
        .build();

    assertThat(segmentLevelMultiMap.getSegmentLevels()).isEqualTo(ImmutableSet.of(0, 1));
    assertThat(segmentLevelMultiMap.getSegmentsInLevel(0)).containsExactly(SEGMENT_0);
    assertThat(segmentLevelMultiMap.getSegmentsInLevel(1)).containsExactly(SEGMENT_1);
  }

  @Test
  public void builder_addAll() {
    SegmentLevelMultiMap segmentLevelMultiMap = SegmentLevelMultiMap.builder()
        .addAll(ImmutableList.of(SEGMENT_1, SEGMENT_0))
        .build();

    assertThat(segmentLevelMultiMap.getSegmentLevels()).isEqualTo(ImmutableSet.of(0, 1));
    assertThat(segmentLevelMultiMap.getSegmentsInLevel(0)).containsExactly(SEGMENT_0);
    assertThat(segmentLevelMultiMap.getSegmentsInLevel(1)).containsExactly(SEGMENT_1);
  }
}
