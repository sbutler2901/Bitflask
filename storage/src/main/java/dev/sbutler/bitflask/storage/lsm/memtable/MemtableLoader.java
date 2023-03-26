package dev.sbutler.bitflask.storage.lsm.memtable;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.exceptions.StorageLoadException;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import dev.sbutler.bitflask.storage.lsm.entry.EntryReader;
import dev.sbutler.bitflask.storage.lsm.entry.EntryUtils;
import java.io.IOException;
import java.util.SortedMap;
import javax.inject.Inject;

/**
 * Creates a Memtable by loading or truncating previous values based on the
 * {@link dev.sbutler.bitflask.storage.configuration.StorageLoadingMode} specified at startup.
 */
public final class MemtableLoader {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final StorageConfigurations configurations;
  private final MemtableFactory memtableFactory;

  @Inject
  MemtableLoader(StorageConfigurations configurations, MemtableFactory memtableFactory) {
    this.configurations = configurations;
    this.memtableFactory = memtableFactory;
  }

  /**
   * Creates a Memtable by loading or truncating previous values based on the
   * {@link dev.sbutler.bitflask.storage.configuration.StorageLoadingMode} specified at startup.
   */
  public Memtable load() {
    return switch (configurations.getStorageLoadingMode()) {
      case TRUNCATE -> createWithTruncation();
      case LOAD -> createWithLoading();
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
      logger.atInfo()
          .log("Created Memtable with [%d] pre-existing entries.", keyEntryMap.size());
      return memtable;
    } catch (IOException e) {
      throw new StorageLoadException("Failed to create Memtable with loading", e);
    }
  }

  /**
   * Loads all entries from the pre-existing {@link WriteAheadLog} file.
   */
  private ImmutableList<Entry> loadEntries() {
    EntryReader entryReader = EntryReader.create(memtableFactory.getWriteAheadLogPath());
    try {
      return entryReader.readAllEntriesFromOffset(0L);
    } catch (IOException e) {
      throw new StorageLoadException("Failed to load entries from WriteAheadLog", e);
    }
  }
}
