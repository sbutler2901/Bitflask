package dev.sbutler.bitflask.storage.lsm;

import static com.google.common.truth.Truth8.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableListMultimap;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import dev.sbutler.bitflask.storage.lsm.memtable.Memtable;
import dev.sbutler.bitflask.storage.lsm.memtable.WriteAheadLog;
import dev.sbutler.bitflask.storage.lsm.segment.Segment;
import dev.sbutler.bitflask.storage.lsm.segment.SegmentLevelMultiMap;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LSMTreeReaderTest {

  Entry ENTRY_0 = new Entry(Instant.now().getEpochSecond(), "key0", "value0");
  Segment SEGMENT_0 = mock(Segment.class);
  Segment SEGMENT_1 = mock(Segment.class);

  Memtable MEMTABLE = Memtable.create(mock(WriteAheadLog.class));
  SegmentLevelMultiMap MULTI_MAP = SegmentLevelMultiMap.create(
      ImmutableListMultimap.of(0, SEGMENT_0, 1, SEGMENT_1));

  LSMTreeStateManager stateManager = new LSMTreeStateManager();

  LSMTreeReader reader = new LSMTreeReader(stateManager, Thread.ofVirtual().factory());

  @BeforeEach
  public void beforeEach() {
    try (var ignored = stateManager.getAndLockCurrentState()) {
      stateManager.updateCurrentState(MEMTABLE, MULTI_MAP);
    }
  }

  @Test
  public void read_entryNotFound() {
    Optional<Entry> readValue = reader.read(ENTRY_0.key());

    assertThat(readValue).isEmpty();
  }

  @Test
  public void read_entryInMemtable() throws Exception {
    MEMTABLE.write(ENTRY_0);

    Optional<Entry> readValue = reader.read(ENTRY_0.key());

    assertThat(readValue).hasValue(ENTRY_0);
  }

  @Test
  public void read_entryInLevelZeroSegment() throws IOException {
    when(SEGMENT_0.mightContain(anyString())).thenReturn(true);
    when(SEGMENT_0.readEntry(anyString())).thenReturn(Optional.of(ENTRY_0));

    Optional<Entry> readValue = reader.read("key");

    assertThat(readValue).hasValue(ENTRY_0);
    verify(SEGMENT_1, times(0)).mightContain(anyString());
    verify(SEGMENT_1, times(0)).readEntry(anyString());
  }

  @Test
  public void read_entryInLevelOneSegment() throws IOException {
    when(SEGMENT_0.mightContain(anyString())).thenReturn(false);
    when(SEGMENT_1.mightContain(anyString())).thenReturn(true);
    when(SEGMENT_1.readEntry(anyString())).thenReturn(Optional.of(ENTRY_0));

    Optional<Entry> readValue = reader.read("key");

    assertThat(readValue).hasValue(ENTRY_0);
    verify(SEGMENT_0, times(1)).mightContain(anyString());
    verify(SEGMENT_0, times(0)).readEntry(anyString());
    verify(SEGMENT_1, times(1)).mightContain(anyString());
    verify(SEGMENT_1, times(1)).readEntry(anyString());
  }
}
