package dev.sbutler.bitflask.storage.lsm;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class LSMTreeTest {

  private final LSMTreeReader reader = mock(LSMTreeReader.class);
  private final LSMTreeWriter writer = mock(LSMTreeWriter.class);

  private final LSMTree lsmTree = new LSMTree(reader, writer);

  @Test
  public void read_entryFound_returnsValue() {
    Entry entry = new Entry(Instant.now().getEpochSecond(), "key", "value");
    when(reader.read(anyString())).thenReturn(Optional.of(entry));

    Optional<String> readValue = lsmTree.read("key");

    assertThat(readValue).hasValue(entry.value());
    verify(reader, times(1)).read("key");
  }

  @Test
  public void read_entryNotFound_returnsEmpty() {
    when(reader.read(anyString())).thenReturn(Optional.empty());

    Optional<String> readValue = lsmTree.read("key");

    assertThat(readValue).isEmpty();
    verify(reader, times(1)).read("key");
  }

  @Test
  public void write() {
    lsmTree.write("key", "value");

    var captor = ArgumentCaptor.forClass(Entry.class);
    verify(writer, times(1)).write(captor.capture());
    assertThat(captor.getValue().key()).isEqualTo("key");
    assertThat(captor.getValue().value()).isEqualTo("value");
  }

  @Test
  public void delete() {
    lsmTree.delete("key");

    var captor = ArgumentCaptor.forClass(Entry.class);
    verify(writer, times(1)).write(captor.capture());
    assertThat(captor.getValue().key()).isEqualTo("key");
    assertThat(captor.getValue().value()).isEqualTo("");
  }
}
