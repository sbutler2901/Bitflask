package dev.sbutler.bitflask.storage.lsm.memtable;

import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.exceptions.StorageLoadException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.TreeMap;
import javax.inject.Inject;

public final class MemtableLoader {

  private final StorageConfigurations configurations;

  @Inject
  MemtableLoader(StorageConfigurations configurations) {
    this.configurations = configurations;
  }

  public Memtable load() {
    return switch (configurations.getStorageLoadingMode()) {
      case TRUNCATE -> createMemtableWithTruncation();
      case LOAD -> createMemtableWithLoading();
    };
  }

  private Memtable createMemtableWithTruncation() {
    try {
      WriteAheadLog writeAheadLog = WriteAheadLog.create(getWriteAheadLogPath());
      return Memtable.create(writeAheadLog);
    } catch (IOException e) {
      throw new StorageLoadException("Failed to create Memtable with truncation", e);
    }
  }

  private Memtable createMemtableWithLoading() {
    // TODO: load entries to populate Memtable
    try {
      WriteAheadLog writeAheadLog = WriteAheadLog.createFromPreExisting(getWriteAheadLogPath());
      return Memtable.create(new TreeMap<>(), writeAheadLog);
    } catch (IOException e) {
      throw new StorageLoadException("Failed to create Memtable with loading", e);
    }
  }

  private Path getWriteAheadLogPath() {
    return Path.of(
        configurations.getStorageStoreDirectoryPath().toString(),
        WriteAheadLog.FILE_NAME + WriteAheadLog.FILE_EXTENSION);
  }
}
