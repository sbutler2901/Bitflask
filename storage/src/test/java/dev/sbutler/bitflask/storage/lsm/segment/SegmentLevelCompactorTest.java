package dev.sbutler.bitflask.storage.lsm.segment;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import dev.sbutler.bitflask.storage.exceptions.StorageCompactionException;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import dev.sbutler.bitflask.storage.lsm.segment.Segment.PathsForDeletion;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.ThreadFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

@SuppressWarnings("unchecked")
public class SegmentLevelCompactorTest {

  private final int SEGMENT_LEVEL = 0;

  private final Path SEGMENT_PATH_0 = Path.of("/tmp/segment_0.seg");
  private final Path SEGMENT_INDEX_PATH_0 = Path.of("/tmp/index_0.idx");
  private final Path SEGMENT_PATH_1 = Path.of("/tmp/segment_1.seg");
  private final Path SEGMENT_INDEX_PATH_1 = Path.of("/tmp/index_1.idx");

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
    when(segment_0.getSegmentLevel()).thenReturn(SEGMENT_LEVEL);
    when(segment_0.getSegmentNumber()).thenReturn(0);
    when(segment_0.readAllEntries()).thenReturn(ImmutableList.of(ENTRY_0));
    when(segment_0.getPathsForDeletion())
        .thenReturn(new PathsForDeletion(SEGMENT_PATH_0, SEGMENT_INDEX_PATH_0));

    when(segment_1.getSegmentLevel()).thenReturn(SEGMENT_LEVEL + 1);
    when(segment_1.getSegmentNumber()).thenReturn(1);
    when(segment_1.readAllEntries()).thenReturn(ImmutableList.of(ENTRY_1));
    when(segment_1.getPathsForDeletion())
        .thenReturn(new PathsForDeletion(SEGMENT_PATH_1, SEGMENT_INDEX_PATH_1));

    segmentLevelMultiMap = SegmentLevelMultiMap.builder()
        .add(segment_0)
        .add(segment_1)
        .build();
  }

  @Test
  public void compactSegmentLevel_success() throws Exception {
    int nextSegmentLevel = SEGMENT_LEVEL + 1;
    when(newSegment.getSegmentLevel()).thenReturn(nextSegmentLevel);
    when(newSegment.getSegmentNumber()).thenReturn(2);
    when(segmentFactory.create(any(), anyInt())).thenReturn(newSegment);

    SegmentLevelMultiMap compactedMap;
    try (MockedStatic<Files> ignored = mockStatic(Files.class)) {
      compactedMap = compactor.compactSegmentLevel(
          segmentLevelMultiMap, SEGMENT_LEVEL);
    }

    assertThat(compactedMap.getSegmentLevels()).containsExactly(nextSegmentLevel);
    assertThat(compactedMap.getSegmentsInLevel(nextSegmentLevel))
        .containsExactly(segment_1, newSegment);

    ArgumentCaptor<ImmutableSortedMap<String, Entry>> keyEntryMapCaptor =
        ArgumentCaptor.forClass(ImmutableSortedMap.class);
    ArgumentCaptor<Integer> segmentLevelCaptor = ArgumentCaptor.forClass(Integer.class);
    verify(segmentFactory, times(1))
        .create(keyEntryMapCaptor.capture(), segmentLevelCaptor.capture());
    assertThat(keyEntryMapCaptor.getValue()).isEqualTo(
        ImmutableSortedMap.of(ENTRY_0.key(), ENTRY_0));
    assertThat(segmentLevelCaptor.getValue()).isEqualTo(nextSegmentLevel);
  }

  @Test
  public void compactSegmentLevel_segmentDeletionFailure_indexDeletionSkipped() throws Exception {
    int nextSegmentLevel = SEGMENT_LEVEL + 1;
    when(newSegment.getSegmentLevel()).thenReturn(nextSegmentLevel);
    when(newSegment.getSegmentNumber()).thenReturn(2);
    when(segmentFactory.create(any(), anyInt())).thenReturn(newSegment);

    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      filesMockedStatic.when(() -> Files.delete(SEGMENT_PATH_0))
          .thenThrow(new IOException("test"));

      compactor.compactSegmentLevel(
          segmentLevelMultiMap, SEGMENT_LEVEL);

      filesMockedStatic.verify(() -> Files.delete(eq(SEGMENT_PATH_0)), times(1));
      filesMockedStatic.verify(() -> Files.delete(eq(SEGMENT_INDEX_PATH_0)), times(0));
    }
  }

  @Test
  public void compactSegmentLevel_indexDeletionFailure() throws Exception {
    int nextSegmentLevel = SEGMENT_LEVEL + 1;
    when(newSegment.getSegmentLevel()).thenReturn(nextSegmentLevel);
    when(newSegment.getSegmentNumber()).thenReturn(2);
    when(segmentFactory.create(any(), anyInt())).thenReturn(newSegment);

    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      filesMockedStatic.when(() -> Files.delete(SEGMENT_INDEX_PATH_0))
          .thenThrow(new IOException("test"));

      compactor.compactSegmentLevel(
          segmentLevelMultiMap, SEGMENT_LEVEL);

      filesMockedStatic.verify(() -> Files.delete(eq(SEGMENT_PATH_0)), times(1));
      filesMockedStatic.verify(() -> Files.delete(eq(SEGMENT_INDEX_PATH_0)), times(1));
    }
  }

  @Test
  public void compactSegmentLevel_segmentReadAllEntriesThrowsException_throwStorageCompactionException()
      throws Exception {
    RuntimeException runtimeException = new RuntimeException("test");
    when(segment_0.readAllEntries()).thenThrow(runtimeException);

    StorageCompactionException exception = assertThrows(StorageCompactionException.class,
        () -> compactor.compactSegmentLevel(segmentLevelMultiMap, SEGMENT_LEVEL));

    assertThat(exception).hasCauseThat().isEqualTo(runtimeException);
    assertThat(exception).hasMessageThat().isEqualTo("Failed getting all entries in segment level");
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
