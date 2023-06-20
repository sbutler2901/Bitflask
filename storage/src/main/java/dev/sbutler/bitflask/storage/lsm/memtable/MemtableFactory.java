package dev.sbutler.bitflask.storage.lsm.memtable;

import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.SortedMap;

/**
 * Factory for creating {@link Memtable}s and their associated {@link WriteAheadLog}.
 */
public final class MemtableFactory {

  private final StorageConfigurations configurations;

  @Inject
  MemtableFactory(StorageConfigurations configurations) {
    this.configurations = configurations;
  }

  /**
   * Creates a new {@link Memtable}.
   *
   * <p>The Memtable's associated {@link WriteAheadLog} will truncate any pre-existing file.
   */
  public Memtable create() throws IOException {
    WriteAheadLog writeAheadLog = WriteAheadLog.create(getWriteAheadLogPath());
    return Memtable.create(writeAheadLog);
  }

  /**
   * Creates a {@link Memtable} using the keyEntryMap.
   *
   * <p>The Memtable's associated {@link WriteAheadLog} will append to any pre-existing file.
   */
  Memtable createWithLoading(SortedMap<String, Entry> keyEntryMap) throws IOException {
    WriteAheadLog writeAheadLog = WriteAheadLog.createFromPreExisting(getWriteAheadLogPath());
    return Memtable.create(keyEntryMap, writeAheadLog);
  }


  Path getWriteAheadLogPath() {
    return Path.of(
        configurations.getStoreDirectoryPath().toString(),
        String.format("%s.%s", WriteAheadLog.FILE_NAME, WriteAheadLog.FILE_EXTENSION));
  }
}
