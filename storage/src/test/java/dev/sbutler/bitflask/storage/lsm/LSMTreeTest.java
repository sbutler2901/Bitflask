package dev.sbutler.bitflask.storage.lsm;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import dev.sbutler.bitflask.storage.exceptions.StorageException;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class LSMTreeTest {

  private final static Entry ENTRY = new Entry(Instant.now().getEpochSecond(), "key", "value");
  private final static Entry DELETED_ENTRY = new Entry(Instant.now().getEpochSecond(), "key", "");

  private final ListeningScheduledExecutorService scheduledExecutorService = mock(
      ListeningScheduledExecutorService.class);
  private final LSMTreeReader reader = mock(LSMTreeReader.class);
  private final LSMTreeWriter writer = mock(LSMTreeWriter.class);

  private final LSMTree lsmTree =
      new LSMTree(scheduledExecutorService, reader, writer);

  @Test
  public void read_entryFound_returnsValue() {
    when(reader.read(anyString())).thenReturn(Optional.of(ENTRY));

    Optional<String> readValue = lsmTree.read("key");

    assertThat(readValue).hasValue(ENTRY.value());
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
  public void read_entryDeleted_returnsEmpty() {
    when(reader.read(anyString())).thenReturn(Optional.of(DELETED_ENTRY));

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

  @Test
  public void close() {
    lsmTree.close();

    assertThrows(StorageException.class, () -> lsmTree.read("key"));
    assertThrows(StorageException.class, () -> lsmTree.write("key", "value"));
    assertThrows(StorageException.class, () -> lsmTree.delete("key"));

    verify(scheduledExecutorService, times(1)).close();
  }
}
