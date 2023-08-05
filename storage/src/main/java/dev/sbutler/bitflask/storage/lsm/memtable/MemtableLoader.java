package dev.sbutler.bitflask.storage.lsm.memtable;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.config.StorageConfig;
import dev.sbutler.bitflask.storage.exceptions.StorageLoadException;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import dev.sbutler.bitflask.storage.lsm.entry.EntryReader;
import dev.sbutler.bitflask.storage.lsm.entry.EntryUtils;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.SortedMap;

/**
 * Creates a Memtable by loading or truncating previous values based on the {@link
 * StorageConfig.LoadingMode} specified at startup.
 */
public final class MemtableLoader {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final StorageConfig storageConfig;
  private final MemtableFactory memtableFactory;

  @Inject
  MemtableLoader(StorageConfig storageConfig, MemtableFactory memtableFactory) {
    this.storageConfig = storageConfig;
    this.memtableFactory = memtableFactory;
  }

  /**
   * Creates a Memtable by loading or truncating previous values based on the {@link
   * StorageConfig.LoadingMode} specified at startup.
   */
  public Memtable load() {
    return switch (storageConfig.getLoadingMode()) {
      case LOAD -> createWithLoading();
      case TRUNCATE -> createWithTruncation();
      case UNRECOGNIZED -> throw new StorageLoadException("Unrecognized StorageConfig.LoadingMode");
    };
  }

  private Memtable createWithTruncation() {
    try {
      Memtable memtable = memtableFactory.create();
      logger.atInfo().log("Created Memtable with write ahead log truncation.");
      return memtable;
    } catch (IOException e) {
      throw new StorageLoadException("Failed to create Memtable with truncation", e);
    }
  }

  private Memtable createWithLoading() {
    ImmutableList<Entry> loadEntries = loadEntries();
    SortedMap<String, Entry> keyEntryMap = EntryUtils.buildKeyEntryMap(loadEntries);

    try {
      Memtable memtable = memtableFactory.createWithLoading(keyEntryMap);
      logger.atInfo().log("Created Memtable with [%d] pre-existing entries.", keyEntryMap.size());
      return memtable;
    } catch (IOException e) {
      throw new StorageLoadException("Failed to create Memtable with loading", e);
    }
  }

  /** Loads all entries from the pre-existing {@link WriteAheadLog} file. */
  private ImmutableList<Entry> loadEntries() {
    EntryReader entryReader = EntryReader.create(memtableFactory.getWriteAheadLogPath());
    try {
      return entryReader.readAllEntriesFromOffset(0L);
    } catch (IOException e) {
      throw new StorageLoadException("Failed to load entries from WriteAheadLog", e);
    }
  }
}
