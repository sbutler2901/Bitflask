package dev.sbutler.bitflask.storage.lsm.segment;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import dev.sbutler.bitflask.storage.exceptions.StorageCompactionException;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ThreadFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@SuppressWarnings("unchecked")
public class SegmentLevelCompactorTest {

  private final int SEGMENT_LEVEL = 0;

  private final Entry ENTRY_0 = new Entry(Instant.now().getEpochSecond(), "key0", "value0");
  private final Entry ENTRY_1 = new Entry(Instant.now().getEpochSecond(), "key1", "value1");

  private final Segment segment_0 = mock(Segment.class);
  private final Segment segment_1 = mock(Segment.class);
  private final Segment newSegment = mock(Segment.class);

  private SegmentLevelMultiMap segmentLevelMultiMap;

  private final ThreadFactory threadFactory = Thread.ofVirtual().factory();
  private final SegmentFactory segmentFactory = mock(SegmentFactory.class);

  private final SegmentLevelCompactor compactor =
      new SegmentLevelCompactor(threadFactory, segmentFactory);

  @BeforeEach
  public void beforeEach() throws Exception {
    when(segment_0.getSegmentLevel()).thenReturn(0);
    when(segment_1.getSegmentLevel()).thenReturn(1);

    when(segment_0.readAllEntries()).thenReturn(ImmutableList.of(ENTRY_0));
    when(segment_1.readAllEntries()).thenReturn(ImmutableList.of(ENTRY_1));

    segmentLevelMultiMap = SegmentLevelMultiMap.builder()
        .add(segment_0)
        .add(segment_1)
        .build();
  }

  @Test
  public void compactSegmentLevel_success() throws Exception {
    when(newSegment.getSegmentLevel()).thenReturn(SEGMENT_LEVEL + 1);
    when(segmentFactory.create(any(), anyInt())).thenReturn(newSegment);

    SegmentLevelMultiMap compactedMap = compactor.compactSegmentLevel(segmentLevelMultiMap, 0);

    assertThat(compactedMap.getSegmentLevels()).containsExactly(SEGMENT_LEVEL + 1);
    assertThat(compactedMap.getSegmentsInLevel(SEGMENT_LEVEL + 1))
        .containsExactly(segment_1, newSegment);

    ArgumentCaptor<ImmutableSortedMap<String, Entry>> captor =
        ArgumentCaptor.forClass(ImmutableSortedMap.class);
    verify(segmentFactory, times(1)).create(captor.capture(), anyInt());
    assertThat(captor.getValue()).isEqualTo(ImmutableSortedMap.of(ENTRY_0.key(), ENTRY_0));
  }

  @Test
  public void compactSegmentLevel_segmentFactoryThrowsIoException_throwStorageCompactionException()
      throws Exception {
    IOException ioException = new IOException("test");
    when(segmentFactory.create(any(), anyInt())).thenThrow(ioException);

    StorageCompactionException exception = assertThrows(StorageCompactionException.class,
        () -> compactor.compactSegmentLevel(segmentLevelMultiMap, SEGMENT_LEVEL));

    assertThat(exception).hasCauseThat().isEqualTo(ioException);
    assertThat(exception).hasMessageThat().isEqualTo("Failed creating new segment");
  }
}
