package dev.sbutler.bitflask.storage.lsm.memtable;

import dev.sbutler.bitflask.storage.exceptions.StorageException;
import java.io.IOException;
import javax.inject.Inject;

public final class MemtableLoader {

  private final WriteAheadLog.Factory writeAheadLogFactory;

  @Inject
  MemtableLoader(WriteAheadLog.Factory writeAheadLogFactory) {
    this.writeAheadLogFactory = writeAheadLogFactory;
  }

  public Memtable load() {
    // TODO: implement proper loading from disk
    try {
      WriteAheadLog writeAheadLog = writeAheadLogFactory.create();
      return Memtable.create(writeAheadLog);
    } catch (IOException e) {
      throw new StorageException(e);
    }
  }
}
