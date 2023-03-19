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
import org.junit.jupiter.api.Test;

public class MemtableTest {

  private final WriteAheadLog writeAheadLog = mock(WriteAheadLog.class);


  @Test
  public void read_presentEntry_returnsValue() {
    Entry entry = new Entry(Instant.now().getEpochSecond(), "key", "value");
    SortedMap<String, Entry> keyEntryMap = new TreeMap<>();
    keyEntryMap.put(entry.key(), entry);
    Memtable memtable = Memtable.create(keyEntryMap, writeAheadLog);

    assertThat(memtable.read(entry.key())).hasValue(entry);
  }

  @Test
  public void read_absentEntry_returnsEmpty() {
    String key = "key";
    Memtable memtable = Memtable.create(writeAheadLog);

    assertThat(memtable.read(key)).isEmpty();
  }

  @Test
  public void write() throws Exception {
    Entry entry = new Entry(Instant.now().getEpochSecond(), "key", "value");
    Memtable memtable = Memtable.create(writeAheadLog);

    memtable.write(entry);

    assertThat(memtable.contains(entry.key())).isTrue();
    assertThat(memtable.read(entry.key())).hasValue(entry);
    assertThat(memtable.getSize()).isEqualTo(entry.getNumBytesSize());
    verify(writeAheadLog, times(1)).append(entry);
  }

  @Test
  public void write_deletePreExistingEntry() throws Exception {
    Entry entry0 = new Entry(Instant.now().getEpochSecond(), "key", "value");
    Entry entry1 = new Entry(Instant.now().getEpochSecond(), "key", "");
    Memtable memtable = Memtable.create(writeAheadLog);

    memtable.write(entry0);
    assertThat(memtable.getSize()).isEqualTo(entry0.getNumBytesSize());

    memtable.write(entry1);

    assertThat(memtable.contains(entry1.key())).isTrue();
    assertThat(memtable.read(entry1.key())).hasValue(entry1);
    assertThat(memtable.getSize()).isEqualTo(entry1.getNumBytesSize());

    verify(writeAheadLog, times(1)).append(entry0);
    verify(writeAheadLog, times(1)).append(entry1);
  }

  @Test
  public void write_overwritePreExistingEntry() throws Exception {
    Entry entry0 = new Entry(Instant.now().getEpochSecond(), "key", "value");
    Entry entry1 = new Entry(Instant.now().getEpochSecond(), "key", "newValue");
    Memtable memtable = Memtable.create(writeAheadLog);

    memtable.write(entry0);
    assertThat(memtable.getSize()).isEqualTo(entry0.getNumBytesSize());

    memtable.write(entry1);

    assertThat(memtable.contains(entry1.key())).isTrue();
    assertThat(memtable.read(entry1.key())).hasValue(entry1);
    assertThat(memtable.getSize()).isEqualTo(entry1.getNumBytesSize());

    verify(writeAheadLog, times(1)).append(entry0);
    verify(writeAheadLog, times(1)).append(entry1);
  }

  @Test
  public void write_multipleUniqueEntries() throws Exception {
    Entry entry0 = new Entry(Instant.now().getEpochSecond(), "key0", "value");
    Entry entry1 = new Entry(Instant.now().getEpochSecond(), "key1", "value");
    Memtable memtable = Memtable.create(writeAheadLog);

    memtable.write(entry0);
    memtable.write(entry1);

    assertThat(memtable.contains(entry0.key())).isTrue();
    assertThat(memtable.contains(entry1.key())).isTrue();
    assertThat(memtable.read(entry0.key())).hasValue(entry0);
    assertThat(memtable.read(entry1.key())).hasValue(entry1);
    assertThat(memtable.getSize()).isEqualTo(entry0.getNumBytesSize() + entry1.getNumBytesSize());

    verify(writeAheadLog, times(1)).append(entry0);
    verify(writeAheadLog, times(1)).append(entry1);
  }

  @Test
  public void contains_presentEntry_returnsTrue() {
    Entry entry = new Entry(Instant.now().getEpochSecond(), "key", "value");
    SortedMap<String, Entry> keyEntryMap = new TreeMap<>();
    keyEntryMap.put(entry.key(), entry);
    Memtable memtable = Memtable.create(keyEntryMap, writeAheadLog);

    assertThat(memtable.contains(entry.key())).isTrue();
  }

  @Test
  public void contains_absentEntry_returnsFalse() {
    String key = "key";
    Memtable memtable = Memtable.create(writeAheadLog);

    assertThat(memtable.contains(key)).isFalse();
  }

  @Test
  public void flush() {
    Entry entry0 = new Entry(Instant.now().getEpochSecond(), "key0", "value0");
    Entry entry1 = new Entry(Instant.now().getEpochSecond(), "key1", "value1");
    Entry deletedEntry = new Entry(Instant.now().getEpochSecond(), "key", "");
    SortedMap<String, Entry> keyEntryMap = new TreeMap<>();
    keyEntryMap.put(entry0.key(), entry0);
    keyEntryMap.put(entry1.key(), entry1);
    keyEntryMap.put(deletedEntry.key(), deletedEntry);
    Memtable memtable = Memtable.create(keyEntryMap, writeAheadLog);

    ImmutableSortedMap<String, Entry> flushedKeyEntryMap = memtable.flush();

    assertThat(flushedKeyEntryMap.containsKey(entry0.key())).isTrue();
    assertThat(flushedKeyEntryMap.get(entry0.key())).isEqualTo(entry0);

    assertThat(flushedKeyEntryMap.containsKey(entry1.key())).isTrue();
    assertThat(flushedKeyEntryMap.get(entry1.key())).isEqualTo(entry1);

    assertThat(flushedKeyEntryMap.containsKey(deletedEntry.key())).isTrue();
    assertThat(flushedKeyEntryMap.get(deletedEntry.key())).isEqualTo(deletedEntry);
  }
}
