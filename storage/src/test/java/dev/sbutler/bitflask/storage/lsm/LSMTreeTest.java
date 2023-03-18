package dev.sbutler.bitflask.storage.lsm;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import dev.sbutler.bitflask.storage.exceptions.StorageLoadException;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class LSMTreeTest {

  private final ListeningScheduledExecutorService scheduledExecutorService = mock(
      ListeningScheduledExecutorService.class);
  private final LSMTreeReader reader = mock(LSMTreeReader.class);
  private final LSMTreeWriter writer = mock(LSMTreeWriter.class);
  private final LSMTreeLoader loader = mock(LSMTreeLoader.class);
  private final LSMTreeCompactor compactor = mock(LSMTreeCompactor.class);

  private final LSMTree lsmTree = new LSMTree(
      scheduledExecutorService, reader, writer, loader, compactor);

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

  @Test
  public void load() {
    lsmTree.load();

    verify(loader, times(1)).load();
    verify(scheduledExecutorService, times(1)).scheduleWithFixedDelay(any(), any(), any());
  }

  @Test
  public void load_multipleCalls_throwsStorageLoadException() {
    lsmTree.load();

    StorageLoadException e = assertThrows(StorageLoadException.class, lsmTree::load);

    assertThat(e).hasMessageThat().isEqualTo("LSMTree should only be loaded once at startup.");
    verify(loader, times(1)).load();
    verify(scheduledExecutorService, times(1)).scheduleWithFixedDelay(any(), any(), any());
  }
}
