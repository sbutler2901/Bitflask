package dev.sbutler.bitflask.storage.lsm.memtable;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.exceptions.StorageLoadException;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import dev.sbutler.bitflask.storage.lsm.entry.EntryReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.inject.Inject;

/**
 * Creates a Memtable by loading or truncating previous values based on the
 * {@link dev.sbutler.bitflask.storage.configuration.StorageLoadingMode} specified at startup.
 */
public final class MemtableLoader {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final StorageConfigurations configurations;

  @Inject
  MemtableLoader(StorageConfigurations configurations) {
    this.configurations = configurations;
  }

  /**
   * Creates a Memtable by loading or truncating previous values based on the
   * {@link dev.sbutler.bitflask.storage.configuration.StorageLoadingMode} specified at startup.
   */
  public Memtable load() {
    return switch (configurations.getStorageLoadingMode()) {
      case TRUNCATE -> createMemtableWithTruncation();
      case LOAD -> createMemtableWithLoading();
    };
  }

  private Memtable createMemtableWithTruncation() {
    try {
      WriteAheadLog writeAheadLog = WriteAheadLog.create(getWriteAheadLogPath());
      logger.atInfo().log("Created Memtable with write ahead log truncation.");
      return Memtable.create(writeAheadLog);
    } catch (IOException e) {
      throw new StorageLoadException("Failed to create Memtable with truncation", e);
    }
  }

  private Memtable createMemtableWithLoading() {
    try {
      SortedMap<String, Entry> loadedKeyEntryMap = loadKeyEntryMap();
      WriteAheadLog writeAheadLog = WriteAheadLog.createFromPreExisting(getWriteAheadLogPath());
      logger.atInfo().log("Loaded [%d] pre-existing Memtable entries.", loadedKeyEntryMap.size());
      return Memtable.create(loadedKeyEntryMap, writeAheadLog);
    } catch (IOException e) {
      throw new StorageLoadException("Failed to create Memtable with loading", e);
    }
  }

  private SortedMap<String, Entry> loadKeyEntryMap() {
    EntryReader entryReader = EntryReader.create(getWriteAheadLogPath());
    ImmutableList<Entry> entries;
    try {
      entries = entryReader.readAllEntriesFromOffset(0L);
    } catch (IOException e) {
      throw new StorageLoadException("Failed to load entries from WriteAheadLog", e);
    }

    SortedMap<String, Entry> keyEntryMap = new TreeMap<>();
    for (var entry : entries) {
      Entry previouslyStoredEntry = keyEntryMap.get(entry.key());
      if (previouslyStoredEntry == null
          || previouslyStoredEntry.creationEpochSeconds() <= entry.creationEpochSeconds()) {
        keyEntryMap.put(entry.key(), entry);
      }
    }
    return keyEntryMap;
  }

  private Path getWriteAheadLogPath() {
    return Path.of(
        configurations.getStorageStoreDirectoryPath().toString(),
        WriteAheadLog.FILE_NAME + WriteAheadLog.FILE_EXTENSION);
  }
}
