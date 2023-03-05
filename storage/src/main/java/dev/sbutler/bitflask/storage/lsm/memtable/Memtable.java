package dev.sbutler.bitflask.storage.lsm.memtable;

import static java.util.function.Predicate.not;

import com.google.common.collect.ImmutableSortedMap;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import java.time.Instant;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * An in memory store of new or updated {@link Entry}s.
 */
public final class Memtable {

  private final SortedMap<String, Entry> keyEntryMap = new TreeMap<>();
  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

  /**
   * Reads the value corresponding to the provided key, if present.
   */
  public Optional<String> read(String key) {
    readWriteLock.readLock().lock();
    try {
      return Optional.ofNullable(keyEntryMap.get(key))
          .map(Entry::value)
          .filter(not(String::isEmpty));
    } finally {
      readWriteLock.readLock().unlock();
    }
  }

  /**
   * Writes the provided key value pair.
   */
  public void write(String key, String value) {
    Entry entry = new Entry(Instant.now().getEpochSecond(), key, value);
    readWriteLock.writeLock().lock();
    try {
      keyEntryMap.put(key, entry);
    } finally {
      readWriteLock.writeLock().unlock();
    }
  }

  /**
   * Deletes the provided key and its associated value.
   */
  public void delete(String key) {
    write(key, "");
  }

  /**
   * Returns true if this Memtable contains an entry for the provided key.
   */
  public boolean contains(String key) {
    return read(key).isPresent();
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
