package dev.sbutler.bitflask.storage.lsm;

import com.google.common.collect.ImmutableListMultimap;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import dev.sbutler.bitflask.storage.lsm.memtable.Memtable;
import dev.sbutler.bitflask.storage.lsm.memtable.WriteAheadLog;
import dev.sbutler.bitflask.storage.lsm.segment.Segment;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.inject.Inject;
import jdk.incubator.concurrent.StructuredTaskScope;

public class LSMTree {

  private final ThreadFactory threadFactory;
  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

  private Memtable memtable;
  private WriteAheadLog writeAheadLog;
  private ImmutableListMultimap<Integer, Segment> sortedBySegmentLevelMultiMap;

  private LSMTree(
      ThreadFactory threadFactory,
      Memtable memtable,
      WriteAheadLog writeAheadLog,
      ImmutableListMultimap<Integer, Segment> sortedBySegmentLevelMultiMap) {
    this.threadFactory = threadFactory;
    this.memtable = memtable;
    this.writeAheadLog = writeAheadLog;
    this.sortedBySegmentLevelMultiMap = sortedBySegmentLevelMultiMap;
  }

  public static class Factory {

    private final ThreadFactory threadFactory;

    @Inject
    Factory(ThreadFactory threadFactory) {
      this.threadFactory = threadFactory;
    }

    public LSMTree create(
        Memtable memtable,
        WriteAheadLog writeAheadLog,
        ImmutableListMultimap<Integer, Segment> segmentLevelMultiMap) {
      ImmutableListMultimap<Integer, Segment> sortedByKey =
          ImmutableListMultimap.<Integer, Segment>builder()
              .orderKeysBy(Integer::compare)
              .putAll(segmentLevelMultiMap)
              .build();
      return new LSMTree(threadFactory, memtable, writeAheadLog, sortedByKey);
    }
  }

  public Optional<String> read(String key) throws InterruptedException, ExecutionException {
    readWriteLock.readLock().lock();
    try {
      Optional<String> memtableEntry = memtable.read(key).map(Entry::value);
      if (memtableEntry.isPresent()) {
        return memtableEntry;
      }
      return readFromSegments(key);
    } finally {
      readWriteLock.readLock().unlock();
    }
  }

  private Optional<String> readFromSegments(String key)
      throws InterruptedException, ExecutionException {
    for (int segmentLevel : sortedBySegmentLevelMultiMap.keySet()) {
      try (var scope = new StructuredTaskScope.ShutdownOnFailure(
          "read-segments-scope", threadFactory)) {
        List<Future<Optional<Entry>>> segmentReadFutures = new ArrayList<>();
        for (Segment segment : sortedBySegmentLevelMultiMap.get(segmentLevel)) {
          if (segment.mightContain(key)) {
            segmentReadFutures.add(scope.fork(() -> segment.readEntry(key)));
          }
        }
        if (!segmentReadFutures.isEmpty()) {
          scope.join();
          scope.throwIfFailed();
          Optional<String> minEntryValue = segmentReadFutures.stream()
              .map(Future::resultNow)
              .filter(Optional::isPresent)
              .map(Optional::get)
              .min(Comparator.comparingLong(Entry::creationEpochSeconds))
              .map(Entry::value);
          if (minEntryValue.isPresent()) {
            return minEntryValue;
          }
        }
      }
    }
    return Optional.empty();
  }

  public void write(String key, String value) throws IOException {
    Entry entry = new Entry(Instant.now().getEpochSecond(), key, value);
    readWriteLock.writeLock().lock();
    try {
      writeAheadLog.append(entry);
      memtable.write(entry);
    } finally {
      readWriteLock.writeLock().unlock();
    }
  }

  public void delete(String key) throws IOException {
    write(key, "");
  }
}
