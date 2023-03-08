package dev.sbutler.bitflask.storage.lsm;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import dev.sbutler.bitflask.storage.exceptions.StorageWriteException;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import dev.sbutler.bitflask.storage.lsm.memtable.Memtable;
import java.io.IOException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

public class LSMTreeWriterTest {

  private final Memtable memtable = mock(Memtable.class);
  private final LSMTreeWriter writer = new LSMTreeWriter(() -> memtable);

  @Test
  public void write() throws Exception {
    Entry entry = new Entry(Instant.now().getEpochSecond(), "key", "value");

    writer.write(entry);

    verify(memtable, times(1)).write(entry);
  }

  @Test
  public void write_memtableThrowsIOException_throwStorageWriteException() throws Exception {
    Entry entry = new Entry(Instant.now().getEpochSecond(), "key", "value");
    IOException ioException = new IOException("test");
    doThrow(ioException).when(memtable).write(any());

    StorageWriteException e =
        assertThrows(StorageWriteException.class, () -> writer.write(entry));

    assertThat(e).hasCauseThat().isEqualTo(ioException);
  }
}
