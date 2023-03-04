package dev.sbutler.bitflask.storage.memtable;

import static com.google.common.truth.Truth8.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MemtableTest {

  private Memtable memtable;

  @BeforeEach
  public void beforeEach() {
    memtable = new Memtable();
  }

  @Test
  public void read() {
    String key = "key";
    String value = "value";
    memtable.write(key, value);

    Optional<String> readValue = memtable.read(key);

    assertThat(readValue).hasValue(value);
  }

  @Test
  public void write() {
    String key = "key";
    String value = "value";

    memtable.write(key, value);

    assertThat(memtable.read(key)).hasValue(value);
  }

  @Test
  public void delete() {
    String key = "key";

    memtable.delete(key);

    assertThat(memtable.read(key)).isEmpty();
  }
}
