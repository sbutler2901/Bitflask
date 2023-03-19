package dev.sbutler.bitflask.storage.lsm;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.exceptions.StorageCompactionException;
import dev.sbutler.bitflask.storage.lsm.LSMTreeStateManager.CurrentState;
import dev.sbutler.bitflask.storage.lsm.memtable.Memtable;
import dev.sbutler.bitflask.storage.lsm.memtable.MemtableFactory;
import dev.sbutler.bitflask.storage.lsm.segment.Segment;
import dev.sbutler.bitflask.storage.lsm.segment.SegmentFactory;
import dev.sbutler.bitflask.storage.lsm.segment.SegmentLevelMultiMap;
import dev.sbutler.bitflask.storage.lsm.segment.SegmentLevelMultiMap.Builder;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LSMTreeCompactorTest {

  private final long MEMTABLE_FLUSH_THRESHOLD = 1;
  private final long SEGMENT_LEVEL_FLUSH_THRESHOLD = 1;

  private final Memtable memtable = mock(Memtable.class);
  private final SegmentLevelMultiMap segmentLevelMultiMap = mock(SegmentLevelMultiMap.class);
  private final Segment segment = mock(Segment.class);

  private final StorageConfigurations configurations = mock(StorageConfigurations.class);
  private final LSMTreeStateManager stateManager =
      new LSMTreeStateManager(memtable, segmentLevelMultiMap);
  private final MemtableFactory memtableFactory = mock(MemtableFactory.class);
  private final SegmentFactory segmentFactory = mock(SegmentFactory.class);

  private final LSMTreeCompactor compactor = new LSMTreeCompactor(
      configurations, stateManager, memtableFactory, segmentFactory);

  @BeforeEach
  public void beforeEach() {
    when(configurations.getMemtableFlushThresholdBytes())
        .thenReturn(MEMTABLE_FLUSH_THRESHOLD);
    when(configurations.getSegmentLevelFlushThresholdBytes()).thenReturn(
        SEGMENT_LEVEL_FLUSH_THRESHOLD);
  }

  @Test
  public void flushMemtable_belowThreshold_returnsFalse() {
    when(memtable.getNumBytesSize()).thenReturn(0);

    assertThat(compactor.flushMemtable()).isFalse();
  }

  @Test
  public void flushMemtable_segmentCreated_returnsTrue() throws Exception {
    when(memtable.getNumBytesSize()).thenReturn(MEMTABLE_FLUSH_THRESHOLD);
    when(segmentFactory.create(any())).thenReturn(segment);
    when(memtableFactory.create()).thenReturn(memtable);
    when(segmentLevelMultiMap.toBuilder()).thenReturn(new Builder());

    boolean memtableFlushed = compactor.flushMemtable();

    assertThat(memtableFlushed).isTrue();

    CurrentState currentState = stateManager.getCurrentState();
    assertThat(currentState.getMemtable()).isEqualTo(memtable);
    assertThat(currentState.getSegmentLevelMultiMap().getSegmentsInLevel(0))
        .containsExactly(segment);
  }

  @Test
  public void flushMemtable_segmentFactoryThrowsIOException_throwsStorageCompactionException()
      throws Exception {
    when(memtable.getNumBytesSize()).thenReturn(MEMTABLE_FLUSH_THRESHOLD);
    IOException ioException = new IOException("test");
    when(segmentFactory.create(any())).thenThrow(ioException);

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
}
