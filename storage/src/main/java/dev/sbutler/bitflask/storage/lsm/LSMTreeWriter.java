package dev.sbutler.bitflask.storage.lsm;

import dev.sbutler.bitflask.storage.exceptions.StorageWriteException;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import dev.sbutler.bitflask.storage.lsm.memtable.Memtable;
import dev.sbutler.bitflask.storage.lsm.memtable.WriteAheadLog;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Provider;

/**
 * Handles write related tasks for the {@link LSMTree}.
 */
final class LSMTreeWriter {

  private final Provider<Memtable> memtableProvider;
  private final Provider<WriteAheadLog> writeAheadLogProvider;

  @Inject
  LSMTreeWriter(Provider<Memtable> memtableProvider,
      Provider<WriteAheadLog> writeAheadLogProvider) {
    this.memtableProvider = memtableProvider;
    this.writeAheadLogProvider = writeAheadLogProvider;
  }

  void write(Entry entry) {
    try {
      writeAheadLogProvider.get().append(entry);
    } catch (IOException e) {
      throw new StorageWriteException(e);
    }
    memtableProvider.get().write(entry);
  }
}
