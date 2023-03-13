package dev.sbutler.bitflask.storage.lsm.segment;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.storage.exceptions.StorageLoadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SegmentLevelMultiMapLoaderTest {

  private final SegmentIndex SEGMENT_INDEX_0 = mock(SegmentIndex.class);
  private final SegmentIndex SEGMENT_INDEX_1 = mock(SegmentIndex.class);

  private final Segment SEGMENT_0 = mock(Segment.class);
  private final Segment SEGMENT_1 = mock(Segment.class);

  private final SegmentLoader segmentLoader = mock(SegmentLoader.class);
  private final SegmentIndexLoader segmentIndexLoader = mock(SegmentIndexLoader.class);

  private final SegmentLevelMultiMapLoader loader =
      new SegmentLevelMultiMapLoader(segmentLoader, segmentIndexLoader);

  @BeforeEach
  public void beforeEach() {
    when(SEGMENT_INDEX_0.getSegmentNumber()).thenReturn(0);
    when(SEGMENT_INDEX_1.getSegmentNumber()).thenReturn(1);

    when(SEGMENT_0.getSegmentLevel()).thenReturn(0);
    when(SEGMENT_1.getSegmentLevel()).thenReturn(1);
  }


  @Test
  public void load_success() {
    when(segmentIndexLoader.load()).thenReturn(ImmutableList.of(SEGMENT_INDEX_0, SEGMENT_INDEX_1));
    when(segmentLoader.loadWithIndexes(any())).thenReturn(ImmutableList.of(SEGMENT_0, SEGMENT_1));

    SegmentLevelMultiMap levelMultiMap = loader.load();

    assertThat(levelMultiMap.getSegmentLevels()).containsExactly(0, 1);
    assertThat(levelMultiMap.getSegmentsInLevel(0)).containsExactly(SEGMENT_0);
    assertThat(levelMultiMap.getSegmentsInLevel(1)).containsExactly(SEGMENT_1);
  }

  @Test
  public void load_duplicateSegmentIndexSegmentNumber_throwsStorageLoadException() {
    when(segmentIndexLoader.load()).thenReturn(ImmutableList.of(SEGMENT_INDEX_0, SEGMENT_INDEX_0));

    StorageLoadException e =
        assertThrows(StorageLoadException.class, loader::load);

    assertThat(e).hasMessageThat()
        .isEqualTo(
            String.format("Duplicate segment number [%d] for SegmentIndexes found",
                SEGMENT_INDEX_0.getSegmentNumber()));
  }
}
