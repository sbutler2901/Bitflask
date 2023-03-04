package dev.sbutler.bitflask.storage.memtable;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import com.google.common.collect.ImmutableSortedMap;
import dev.sbutler.bitflask.storage.entry.Entry;
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
    String key = "key";
    String value = "value";
    memtable.write(key, value);

    assertThat(memtable.read(key)).hasValue(value);
  }

  @Test
  public void read_absentEntry_returnsEmpty() {
    String key = "key";

    assertThat(memtable.read(key)).isEmpty();
  }

  @Test
  public void read_deletedEntry_returnsEmpty() {
    String key = "key";
    memtable.delete(key);

    assertThat(memtable.read(key)).isEmpty();
  }

  @Test
  public void write() {
    String key = "key";
    String value = "value";

    memtable.write(key, value);

    assertThat(memtable.read(key)).hasValue(value);
    assertThat(memtable.contains(key)).isTrue();
  }

  @Test
  public void delete() {
    String key = "key";

    memtable.delete(key);

    assertThat(memtable.read(key)).isEmpty();
    assertThat(memtable.contains(key)).isFalse();
  }

  @Test
  public void contains_presentEntry_returnsTrue() {
    String key = "key";
    String value = "value";
    memtable.write(key, value);

    assertThat(memtable.contains(key)).isTrue();
  }

  @Test
  public void contains_absentEntry_returnsFalse() {
    String key = "key";

    assertThat(memtable.contains(key)).isFalse();
  }

  @Test
  public void contains_deletedEntry_returnsFalse() {
    String key = "key";
    memtable.delete(key);

    assertThat(memtable.contains(key)).isFalse();
  }

  @Test
  public void flush() {
    String key0 = "key0", key1 = "key1";
    String value0 = "value0", value1 = "value1";
    memtable.write(key0, value0);
    memtable.write(key1, value1);

    ImmutableSortedMap<String, Entry> flushedKeyEntryMap = memtable.flush();

    assertThat(flushedKeyEntryMap.containsKey(key0)).isTrue();
    assertThat(flushedKeyEntryMap.get(key0).key()).isEqualTo(key0);
    assertThat(flushedKeyEntryMap.get(key0).value()).isEqualTo(value0);

    assertThat(flushedKeyEntryMap.containsKey(key1)).isTrue();
    assertThat(flushedKeyEntryMap.get(key1).key()).isEqualTo(key1);
    assertThat(flushedKeyEntryMap.get(key1).value()).isEqualTo(value1);
  }
}
