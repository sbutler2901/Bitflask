package dev.sbutler.bitflask.storage.lsm;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSortedMap;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.exceptions.StorageCompactionException;
import dev.sbutler.bitflask.storage.lsm.LSMTreeStateManager.CurrentState;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import dev.sbutler.bitflask.storage.lsm.memtable.Memtable;
import dev.sbutler.bitflask.storage.lsm.memtable.MemtableFactory;
import dev.sbutler.bitflask.storage.lsm.segment.Segment;
import dev.sbutler.bitflask.storage.lsm.segment.SegmentFactory;
import dev.sbutler.bitflask.storage.lsm.segment.SegmentLevelCompactor;
import dev.sbutler.bitflask.storage.lsm.segment.SegmentLevelMultiMap;
import java.io.IOException;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LSMTreeCompactorTest {

  private final long MEMTABLE_FLUSH_THRESHOLD = 1;
  private final long SEGMENT_LEVEL_FLUSH_THRESHOLD = 1;

  private final Entry ENTRY_0 = new Entry(Instant.now().getEpochSecond(), "key0", "value0");

  private final Memtable memtable = mock(Memtable.class);
  private final SegmentLevelMultiMap segmentLevelMultiMap = mock(SegmentLevelMultiMap.class);
  private final Segment segment = mock(Segment.class);

  private final StorageConfigurations configurations = mock(StorageConfigurations.class);
  private final LSMTreeStateManager stateManager =
      new LSMTreeStateManager(memtable, segmentLevelMultiMap);
  private final SegmentLevelCompactor segmentLevelCompactor = mock(SegmentLevelCompactor.class);
  private final MemtableFactory memtableFactory = mock(MemtableFactory.class);
  private final SegmentFactory segmentFactory = mock(SegmentFactory.class);

  private final LSMTreeCompactor compactor = new LSMTreeCompactor(
      configurations, stateManager, segmentLevelCompactor, memtableFactory, segmentFactory);

  @BeforeEach
  public void beforeEach() {
    when(configurations.getMemtableFlushThresholdBytes())
        .thenReturn(MEMTABLE_FLUSH_THRESHOLD);
    when(configurations.getSegmentLevelFlushThresholdBytes()).thenReturn(
        SEGMENT_LEVEL_FLUSH_THRESHOLD);
  }

  @Test
  public void run_memtableNotFlushed() {
    when(memtable.getNumBytesSize()).thenReturn(0L);
    mockFirstSegmentLevelOverThreshold(segmentLevelMultiMap, mock(SegmentLevelMultiMap.class));

    compactor.run();

    CurrentState currentState = stateManager.getCurrentState();
    assertThat(currentState.getMemtable()).isEqualTo(memtable);
    assertThat(currentState.getSegmentLevelMultiMap()).isEqualTo(segmentLevelMultiMap);

    verify(segmentLevelCompactor, times(0))
        .compactSegmentLevel(any(), anyInt());
  }

  @Test
  public void run_memtableFlushed_firstSegmentLevelCompacted() throws Exception {
    when(memtable.getNumBytesSize()).thenReturn(0L);
    Memtable newMemtable = mock(Memtable.class);
    SegmentLevelMultiMap firstNewSegmentLevelMultiMap = mock(SegmentLevelMultiMap.class);
    mockMemtableFlushed(newMemtable, firstNewSegmentLevelMultiMap);

    SegmentLevelMultiMap secondNewSegmentLevelMultiMap = mock(SegmentLevelMultiMap.class);
    mockFirstSegmentLevelOverThreshold(firstNewSegmentLevelMultiMap, secondNewSegmentLevelMultiMap);

    compactor.run();

    CurrentState currentState = stateManager.getCurrentState();
    assertThat(currentState.getMemtable()).isEqualTo(newMemtable);
    assertThat(currentState.getSegmentLevelMultiMap()).isEqualTo(secondNewSegmentLevelMultiMap);

    verify(segmentLevelCompactor, times(1))
        .compactSegmentLevel(any(), anyInt());
  }

  @Test
  public void flushMemtable_belowThreshold_returnsFalse() {
    when(memtable.getNumBytesSize()).thenReturn(0L);

    assertThat(compactor.flushMemtable()).isFalse();

    CurrentState currentState = stateManager.getCurrentState();
    assertThat(currentState.getMemtable()).isEqualTo(memtable);
    assertThat(currentState.getSegmentLevelMultiMap()).isEqualTo(segmentLevelMultiMap);
  }

  @Test
  public void flushMemtable_segmentCreated_returnsTrue() throws Exception {
    Memtable newMemtable = mock(Memtable.class);
    SegmentLevelMultiMap newSegmentLevelMultiMap = mock(SegmentLevelMultiMap.class);
    mockMemtableFlushed(newMemtable, newSegmentLevelMultiMap);

    boolean memtableFlushed = compactor.flushMemtable();

    assertThat(memtableFlushed).isTrue();

    CurrentState currentState = stateManager.getCurrentState();
    assertThat(currentState.getMemtable()).isEqualTo(newMemtable);
    assertThat(currentState.getSegmentLevelMultiMap()).isEqualTo(newSegmentLevelMultiMap);
  }

  @Test
  public void flushMemtable_segmentFactoryThrowsIOException_throwsStorageCompactionException()
      throws Exception {
    when(memtable.flush()).thenReturn(ImmutableSortedMap.of(ENTRY_0.key(), ENTRY_0));
    when(memtable.getNumBytesSize()).thenReturn(MEMTABLE_FLUSH_THRESHOLD);
    IOException ioException = new IOException("test");
    when(segmentFactory.create(any(), anyInt(), anyLong())).thenThrow(ioException);

    StorageCompactionException exception =
        assertThrows(StorageCompactionException.class, compactor::flushMemtable);

    assertThat(exception).hasCauseThat().isEqualTo(ioException);
    assertThat(exception).hasMessageThat()
        .isEqualTo("Failed to create new Segment from Memtable");
  }

  @Test
  public void flushMemtable_memtableFactoryThrowsIOException_throwsStorageCompactionException()
      throws Exception {
    when(memtable.getNumBytesSize()).thenReturn(MEMTABLE_FLUSH_THRESHOLD);
    IOException ioException = new IOException("test");
    when(memtableFactory.create()).thenThrow(ioException);

    StorageCompactionException exception =
        assertThrows(StorageCompactionException.class, compactor::flushMemtable);

    assertThat(exception).hasCauseThat().isEqualTo(ioException);
    assertThat(exception).hasMessageThat().isEqualTo("Failed creating new Memtable");
  }

  @Test
  public void compactSegmentLevels_firstLevelUnderThreshold_noCompactionPerformed() {
    when(segmentLevelMultiMap.getNumBytesSizeOfSegmentLevel(anyInt()))
        .thenReturn(SEGMENT_LEVEL_FLUSH_THRESHOLD - 1);

    compactor.compactSegmentLevels();

    verify(segmentLevelCompactor, times(0))
        .compactSegmentLevel(any(), anyInt());

    CurrentState currentState = stateManager.getCurrentState();
    assertThat(currentState.getMemtable()).isEqualTo(memtable);
    assertThat(currentState.getSegmentLevelMultiMap()).isEqualTo(segmentLevelMultiMap);
  }

  @Test
  public void compactSegmentLevels_firstLevelOverThreshold_compactionPerformed() {
    SegmentLevelMultiMap newSegmentLevelMultiMap = mock(SegmentLevelMultiMap.class);
    mockFirstSegmentLevelOverThreshold(segmentLevelMultiMap, newSegmentLevelMultiMap);

    compactor.compactSegmentLevels();

    verify(segmentLevelCompactor, times(1))
        .compactSegmentLevel(any(), anyInt());

    CurrentState currentState = stateManager.getCurrentState();
    assertThat(currentState.getMemtable()).isEqualTo(memtable);
    assertThat(currentState.getSegmentLevelMultiMap()).isEqualTo(newSegmentLevelMultiMap);
  }

  private void mockMemtableFlushed(Memtable newMemtable,
      SegmentLevelMultiMap newSegmentLevelMultiMap) throws Exception {
    when(memtable.flush()).thenReturn(ImmutableSortedMap.of(ENTRY_0.key(), ENTRY_0));
    when(memtable.getNumBytesSize()).thenReturn(MEMTABLE_FLUSH_THRESHOLD);
    when(segmentFactory.create(any(), anyInt(), anyLong())).thenReturn(segment);
    when(memtableFactory.create()).thenReturn(newMemtable);
    SegmentLevelMultiMap.Builder builder = mock(SegmentLevelMultiMap.Builder.class);
    when(builder.add(any())).thenReturn(builder);
    when(builder.build()).thenReturn(newSegmentLevelMultiMap);
    when(segmentLevelMultiMap.toBuilder()).thenReturn(builder);
  }

  private void mockFirstSegmentLevelOverThreshold(SegmentLevelMultiMap overThresholdMap,
      SegmentLevelMultiMap newMap) {
    when(overThresholdMap.getNumBytesSizeOfSegmentLevel(anyInt()))
        .thenReturn(SEGMENT_LEVEL_FLUSH_THRESHOLD);

    when(newMap.getNumBytesSizeOfSegmentLevel(anyInt()))
        .thenReturn(SEGMENT_LEVEL_FLUSH_THRESHOLD - 1);
    when(segmentLevelCompactor.compactSegmentLevel(any(), anyInt()))
        .thenReturn(newMap);
  }
}
