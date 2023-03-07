package dev.sbutler.bitflask.storage.lsm;

import dev.sbutler.bitflask.storage.exceptions.StorageWriteException;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import dev.sbutler.bitflask.storage.lsm.memtable.Memtable;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Provider;

/**
 * Handles write related tasks for the {@link LSMTree}.
 */
final class LSMTreeWriter {

  private final Provider<Memtable> memtableProvider;

  @Inject
  LSMTreeWriter(Provider<Memtable> memtableProvider) {
    this.memtableProvider = memtableProvider;
  }

  void write(Entry entry) {
    try {
      memtableProvider.get().write(entry);
    } catch (IOException e) {
      throw new StorageWriteException(e);
    }
  }
}
