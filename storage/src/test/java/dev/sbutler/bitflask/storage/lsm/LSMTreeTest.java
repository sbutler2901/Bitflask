package dev.sbutler.bitflask.storage.lsm;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class LSMTreeTest {

  private final LSMTreeReader reader = mock(LSMTreeReader.class);
  private final LSMTreeWriter writer = mock(LSMTreeWriter.class);

  private final LSMTree lsmTree = new LSMTree(reader, writer);

  @Test
  public void read() {
    lsmTree.read("key");

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
