package dev.sbutler.bitflask.storage.lsm.memtable;

import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.exceptions.StorageException;
import java.io.IOException;
import java.nio.file.Path;
import javax.inject.Inject;

public final class MemtableLoader {

  private final StorageConfigurations configurations;

  @Inject
  MemtableLoader(StorageConfigurations configurations) {
    this.configurations = configurations;
  }

  public Memtable load() {
    // TODO: implement proper loading from disk
    try {
      // if truncate, load WAL w/o entries and create Memtable,
      // otherwise, load WAL entries, and create Memtable
      WriteAheadLog writeAheadLog = WriteAheadLog.createFromPreExisting(getWriteAheadLogPath());
      return Memtable.create(writeAheadLog);
    } catch (IOException e) {
      throw new StorageException(e);
    }
  }

  private Path getWriteAheadLogPath() {
    return Path.of(
        configurations.getStorageStoreDirectoryPath().toString(),
        WriteAheadLog.FILE_NAME + WriteAheadLog.FILE_EXTENSION);
  }
}
