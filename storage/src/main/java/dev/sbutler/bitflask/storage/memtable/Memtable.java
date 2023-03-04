package dev.sbutler.bitflask.storage.memtable;

import static java.util.function.Predicate.not;

import dev.sbutler.bitflask.storage.entry.Entry;
import java.time.Instant;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * An in memory store of new or updated {@link dev.sbutler.bitflask.storage.entry.Entry}s.
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
}
