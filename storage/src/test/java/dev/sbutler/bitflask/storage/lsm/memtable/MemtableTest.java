package dev.sbutler.bitflask.storage.lsm.memtable;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import com.google.common.collect.ImmutableSortedMap;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MemtableTest {

  private Memtable memtable;

  @BeforeEach
  public void beforeEach() {
    memtable = new Memtable();
  }

  @Test
  public void read_presentEntry_returnsValue() {
    Entry entry = new Entry(Instant.now().getEpochSecond(), "key", "value");

    memtable.write(entry);

    assertThat(memtable.read(entry.key())).hasValue(entry);
  }

  @Test
  public void read_absentEntry_returnsEmpty() {
    String key = "key";

    assertThat(memtable.read(key)).isEmpty();
  }

  @Test
  public void write() {
    Entry entry = new Entry(Instant.now().getEpochSecond(), "key", "value");

    memtable.write(entry);

    assertThat(memtable.contains(entry.key())).isTrue();
    assertThat(memtable.read(entry.key())).hasValue(entry);
  }

  @Test
  public void contains_presentEntry_returnsTrue() {
    Entry entry = new Entry(Instant.now().getEpochSecond(), "key", "value");
    memtable.write(entry);

    assertThat(memtable.contains(entry.key())).isTrue();
  }

  @Test
  public void contains_absentEntry_returnsFalse() {
    String key = "key";

    assertThat(memtable.contains(key)).isFalse();
  }

  @Test
  public void flush() {
    Entry entry0 = new Entry(Instant.now().getEpochSecond(), "key0", "value0");
    Entry entry1 = new Entry(Instant.now().getEpochSecond(), "key1", "value1");
    Entry deletedEntry = new Entry(Instant.now().getEpochSecond(), "key", "");
    memtable.write(entry0);
    memtable.write(entry1);
    memtable.write(deletedEntry);

    ImmutableSortedMap<String, Entry> flushedKeyEntryMap = memtable.flush();

    assertThat(flushedKeyEntryMap.containsKey(entry0.key())).isTrue();
    assertThat(flushedKeyEntryMap.get(entry0.key())).isEqualTo(entry0);

    assertThat(flushedKeyEntryMap.containsKey(entry1.key())).isTrue();
    assertThat(flushedKeyEntryMap.get(entry1.key())).isEqualTo(entry1);

    assertThat(flushedKeyEntryMap.containsKey(deletedEntry.key())).isTrue();
    assertThat(flushedKeyEntryMap.get(deletedEntry.key())).isEqualTo(deletedEntry);
  }
}
