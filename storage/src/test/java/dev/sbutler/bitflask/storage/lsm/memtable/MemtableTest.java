package dev.sbutler.bitflask.storage.lsm.memtable;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableSortedMap;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import java.time.Instant;
import java.util.SortedMap;
import java.util.TreeMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MemtableTest {

  private final Entry ENTRY_0 = new Entry(Instant.now().getEpochSecond(), "key0", "value0");
  private final Entry ENTRY_0_DELETED = new Entry(Instant.now().getEpochSecond(), "key0", "");
  private final Entry ENTRY_1 = new Entry(Instant.now().getEpochSecond(), "key1", "value1");
  private final Entry ENTRY_1_EXTENDED = new Entry(Instant.now().getEpochSecond(), "key1",
      "value1-extended");

  private final SortedMap<String, Entry> KEY_ENTRY_MAP = new TreeMap<>();

  private final WriteAheadLog writeAheadLog = mock(WriteAheadLog.class);

  @BeforeEach
  public void beforeEach() {
    KEY_ENTRY_MAP.put(ENTRY_0.key(), ENTRY_0);
    KEY_ENTRY_MAP.put(ENTRY_1.key(), ENTRY_1);
  }

  @Test
  public void create() {
    Memtable memtable = Memtable.create(writeAheadLog);

    assertThat(memtable.getSize()).isEqualTo(0);
  }

  @Test
  public void create_withKeyEntryMap() {
    Memtable memtable = Memtable.create(KEY_ENTRY_MAP, writeAheadLog);

    assertThat(memtable.contains(ENTRY_0.key())).isTrue();
    assertThat(memtable.contains(ENTRY_1.key())).isTrue();

    assertThat(memtable.read(ENTRY_0.key())).hasValue(ENTRY_0);
    assertThat(memtable.read(ENTRY_1.key())).hasValue(ENTRY_1);

    assertThat(memtable.getSize()).isEqualTo(
        ENTRY_0.getNumBytesSize()
            + ENTRY_1.getNumBytesSize());
  }

  @Test
  public void read_presentEntry_returnsValue() {
    SortedMap<String, Entry> keyEntryMap = new TreeMap<>();
    keyEntryMap.put(ENTRY_0.key(), ENTRY_0);
    Memtable memtable = Memtable.create(keyEntryMap, writeAheadLog);

    assertThat(memtable.read(ENTRY_0.key())).hasValue(ENTRY_0);
  }

  @Test
  public void read_absentEntry_returnsEmpty() {
    String key = "key";
    Memtable memtable = Memtable.create(writeAheadLog);

    assertThat(memtable.read(key)).isEmpty();
  }

  @Test
  public void write() throws Exception {
    Memtable memtable = Memtable.create(writeAheadLog);

    memtable.write(ENTRY_0);

    assertThat(memtable.contains(ENTRY_0.key())).isTrue();
    assertThat(memtable.read(ENTRY_0.key())).hasValue(ENTRY_0);
    assertThat(memtable.getSize()).isEqualTo(ENTRY_0.getNumBytesSize());
    verify(writeAheadLog, times(1)).append(ENTRY_0);
  }

  @Test
  public void write_deletePreExistingEntry() throws Exception {
    Memtable memtable = Memtable.create(writeAheadLog);

    memtable.write(ENTRY_0);
    assertThat(memtable.getSize()).isEqualTo(ENTRY_0.getNumBytesSize());

    memtable.write(ENTRY_0_DELETED);

    assertThat(memtable.contains(ENTRY_0_DELETED.key())).isTrue();
    assertThat(memtable.read(ENTRY_0_DELETED.key())).hasValue(ENTRY_0_DELETED);
    assertThat(memtable.getSize()).isEqualTo(ENTRY_0_DELETED.getNumBytesSize());

    verify(writeAheadLog, times(1)).append(ENTRY_0);
    verify(writeAheadLog, times(1)).append(ENTRY_0_DELETED);
  }

  @Test
  public void write_overwritePreExistingEntry() throws Exception {
    Memtable memtable = Memtable.create(writeAheadLog);

    memtable.write(ENTRY_1);
    assertThat(memtable.getSize()).isEqualTo(ENTRY_0.getNumBytesSize());

    memtable.write(ENTRY_1_EXTENDED);

    assertThat(memtable.contains(ENTRY_1_EXTENDED.key())).isTrue();
    assertThat(memtable.read(ENTRY_1_EXTENDED.key())).hasValue(ENTRY_1_EXTENDED);
    assertThat(memtable.getSize()).isEqualTo(ENTRY_1_EXTENDED.getNumBytesSize());

    verify(writeAheadLog, times(1)).append(ENTRY_1);
    verify(writeAheadLog, times(1)).append(ENTRY_1_EXTENDED);
  }

  @Test
  public void write_multipleUniqueEntries() throws Exception {
    Memtable memtable = Memtable.create(writeAheadLog);

    memtable.write(ENTRY_0);
    memtable.write(ENTRY_1);

    assertThat(memtable.contains(ENTRY_0.key())).isTrue();
    assertThat(memtable.contains(ENTRY_1.key())).isTrue();
    assertThat(memtable.read(ENTRY_0.key())).hasValue(ENTRY_0);
    assertThat(memtable.read(ENTRY_1.key())).hasValue(ENTRY_1);
    assertThat(memtable.getSize()).isEqualTo(ENTRY_0.getNumBytesSize() + ENTRY_1.getNumBytesSize());

    verify(writeAheadLog, times(1)).append(ENTRY_0);
    verify(writeAheadLog, times(1)).append(ENTRY_1);
  }

  @Test
  public void contains_presentEntry_returnsTrue() {
    SortedMap<String, Entry> keyEntryMap = new TreeMap<>();
    keyEntryMap.put(ENTRY_0.key(), ENTRY_0);
    Memtable memtable = Memtable.create(keyEntryMap, writeAheadLog);

    assertThat(memtable.contains(ENTRY_0.key())).isTrue();
  }

  @Test
  public void contains_absentEntry_returnsFalse() {
    String key = "key";
    Memtable memtable = Memtable.create(writeAheadLog);

    assertThat(memtable.contains(key)).isFalse();
  }

  @Test
  public void flush() {
    SortedMap<String, Entry> keyEntryMap = new TreeMap<>();
    keyEntryMap.put(ENTRY_0.key(), ENTRY_0);
    keyEntryMap.put(ENTRY_1.key(), ENTRY_1);
    Memtable memtable = Memtable.create(keyEntryMap, writeAheadLog);

    ImmutableSortedMap<String, Entry> flushedKeyEntryMap = memtable.flush();

    assertThat(flushedKeyEntryMap.containsKey(ENTRY_0.key())).isTrue();
    assertThat(flushedKeyEntryMap.get(ENTRY_0.key())).isEqualTo(ENTRY_0);

    assertThat(flushedKeyEntryMap.containsKey(ENTRY_1.key())).isTrue();
    assertThat(flushedKeyEntryMap.get(ENTRY_1.key())).isEqualTo(ENTRY_1);
  }
}
