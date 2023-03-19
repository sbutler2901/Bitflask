package dev.sbutler.bitflask.storage.lsm.memtable;

import com.google.common.collect.ImmutableSortedMap;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import java.io.IOException;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * An in memory store of new or updated {@link Entry}s with {@link WriteAheadLog} support.
 *
 * <p>Operations are synchronized and can be used in multiple threads concurrently.
 */
public final class Memtable {

  private final SortedMap<String, Entry> keyEntryMap;
  private final WriteAheadLog writeAheadLog;
  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

  private final AtomicInteger currentSize = new AtomicInteger(0);

  private Memtable(SortedMap<String, Entry> keyEntryMap, WriteAheadLog writeAheadLog) {
    this.keyEntryMap = keyEntryMap;
    this.writeAheadLog = writeAheadLog;
  }

  static Memtable create(WriteAheadLog writeAheadLog) {
    return new Memtable(new TreeMap<>(), writeAheadLog);
  }

  static Memtable create(SortedMap<String, Entry> keyEntryMap, WriteAheadLog writeAheadLog) {
    // TODO: update currentSize
    return new Memtable(keyEntryMap, writeAheadLog);
  }

  /**
   * Reads the value corresponding to the provided key, if present.
   */
  public Optional<Entry> read(String key) {
    readWriteLock.readLock().lock();
    try {
      return Optional.ofNullable(keyEntryMap.get(key));
    } finally {
      readWriteLock.readLock().unlock();
    }
  }

  /**
   * Writes the provided {@link Entry}.
   */
  public void write(Entry entry) throws IOException {
    readWriteLock.writeLock().lock();
    try {
      writeAheadLog.append(entry);
      Entry prevEntry = keyEntryMap.put(entry.key(), entry);
      updateSize(entry, Optional.ofNullable(prevEntry));
    } finally {
      readWriteLock.writeLock().unlock();
    }
  }

  private void updateSize(Entry newEntry, Optional<Entry> prevEntry) {
    currentSize.getAndAdd(
        prevEntry
            .map(Entry::getNumBytesSize)
            .map(prevSize -> Math.subtractExact(newEntry.getNumBytesSize(), prevSize))
            .orElseGet(newEntry::getNumBytesSize));
  }

  /**
   * Returns the number of bytes of all {@link Entry}s contained within the Memtable.
   */
  public int getSize() {
    return currentSize.get();
  }

  /**
   * Returns true if this Memtable contains an entry for the provided key.
   */
  public boolean contains(String key) {
    readWriteLock.readLock().lock();
    try {
      return keyEntryMap.containsKey(key);
    } finally {
      readWriteLock.readLock().unlock();
    }
  }

  /**
   * Flushes all key:entry pairs contained within this Memtable.
   */
  public ImmutableSortedMap<String, Entry> flush() {
    readWriteLock.readLock().lock();
    try {
      return ImmutableSortedMap.copyOfSorted(keyEntryMap);
    } finally {
      readWriteLock.readLock().unlock();
    }
  }
}
