package dev.sbutler.bitflask.storage.lsm;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.exceptions.StorageCompactionException;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import dev.sbutler.bitflask.storage.lsm.memtable.Memtable;
import dev.sbutler.bitflask.storage.lsm.memtable.MemtableFactory;
import dev.sbutler.bitflask.storage.lsm.segment.Segment;
import dev.sbutler.bitflask.storage.lsm.segment.SegmentFactory;
import dev.sbutler.bitflask.storage.lsm.segment.SegmentLevelMultiMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import javax.inject.Inject;
import jdk.incubator.concurrent.StructuredTaskScope;

/**
 * Manages periodically compacting an {@link LSMTree}.
 *
 * <p><b>WARNING</b>: only a single instance of the compactor should be running at any given time.
 */
final class LSMTreeCompactor implements Runnable {

  private final ThreadFactory threadFactory;
  private final StorageConfigurations configurations;
  private final LSMTreeStateManager stateManager;
  private final MemtableFactory memtableFactory;
  private final SegmentFactory segmentFactory;

  @Inject
  LSMTreeCompactor(
      ThreadFactory threadFactory,
      StorageConfigurations configurations,
      LSMTreeStateManager stateManager,
      MemtableFactory memtableFactory,
      SegmentFactory segmentFactory) {
    this.threadFactory = threadFactory;
    this.configurations = configurations;
    this.stateManager = stateManager;
    this.memtableFactory = memtableFactory;
    this.segmentFactory = segmentFactory;
  }

  @Override
  public void run() {
    if (flushMemtable()) {
      compactSegmentLevels();
    }
  }

  /**
   * Returns true if the current {@link Memtable} was flushed to a {@link Segment}.
   */
  boolean flushMemtable() {
    try (var currentState = stateManager.getAndLockCurrentState()) {
      if (currentState.getMemtable().getNumBytesSize()
          < configurations.getMemtableFlushThresholdBytes()) {
        return false;
      }

      Segment segmentFromMemtable;
      try {
        segmentFromMemtable = segmentFactory.create(currentState.getMemtable());
      } catch (IOException e) {
        throw new StorageCompactionException("Failed to create new Segment from Memtable", e);
      }

      Memtable newMemtable;
      try {
        newMemtable = memtableFactory.create();
      } catch (IOException e) {
        throw new StorageCompactionException("Failed creating new Memtable", e);
      }

      // add memtable to segment level
      SegmentLevelMultiMap newMultiMap = currentState.getSegmentLevelMultiMap().toBuilder()
          .add(segmentFromMemtable)
          .build();

      // update state and release lock
      stateManager.updateCurrentState(newMemtable, newMultiMap);
    }
    return true;
  }

  /**
   * Iterates the current segment level's compacting each, if their threshold size has been reached,
   * and updates the {@link LSMTreeStateManager} state accordingly.
   */
  private void compactSegmentLevels() {
    // Assumes another compactor thread will not be altering state.
    SegmentLevelMultiMap segmentLevelMultiMap;
    try (var currentState = stateManager.getCurrentState()) {
      segmentLevelMultiMap = currentState.getSegmentLevelMultiMap();
    }

    int segmentLevel = 0;
    for (; segmentLevelMultiMap.getNumBytesSizeOfSegmentLevel(segmentLevel)
        >= configurations.getSegmentLevelFlushThresholdBytes();
        segmentLevel++) {
      segmentLevelMultiMap = compactSegmentLevel(segmentLevelMultiMap, segmentLevel);
    }
    // Don't wait for lock if no compaction occurred
    if (segmentLevel == 0) {
      return;
    }

    try (var currentState = stateManager.getAndLockCurrentState()) {
      stateManager.updateCurrentState(currentState.getMemtable(), segmentLevelMultiMap);
    }
  }

  /**
   * Compacts the provided segment level returning the updated
   * {@link dev.sbutler.bitflask.storage.lsm.segment.SegmentLevelMultiMap}.
   */
  private SegmentLevelMultiMap compactSegmentLevel(
      SegmentLevelMultiMap segmentLevelMultiMap, int segmentLevel) {
    // TODO: evaluate performance
    ImmutableList<Segment> segmentsInLevel = segmentLevelMultiMap.getSegmentsInLevel(segmentLevel);
    ImmutableList<Entry> entriesInLevel = getAllEntriesInLevel(segmentsInLevel);
    ImmutableSortedMap<String, Entry> keyEntryMap = getKeyEntryMap(entriesInLevel);

    Segment newSegment;
    try {
      newSegment = segmentFactory.create(keyEntryMap, segmentLevel + 1);
    } catch (IOException e) {
      throw new StorageCompactionException("Failed creating new segment", e);
    }

    return segmentLevelMultiMap.toBuilder()
        .add(newSegment)
        .build();
  }

  private ImmutableList<Entry> getAllEntriesInLevel(
      ImmutableList<Segment> segmentsInLevel) {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure("compact-segments-level-scope",
        threadFactory)) {
      List<Future<ImmutableList<Entry>>> segmentEntriesFutures =
          new ArrayList<>(segmentsInLevel.size());
      for (var segment : segmentsInLevel) {
        segmentEntriesFutures.add(scope.fork(segment::readAllEntries));
      }

      try {
        scope.join();
        scope.throwIfFailed(e ->
            new StorageCompactionException("Failed getting all entries in segment leve", e));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new StorageCompactionException("Interrupted while loading segment entries", e);
      }

      return segmentEntriesFutures.stream()
          .map(Future::resultNow)
          .flatMap(ImmutableList::stream)
          .collect(toImmutableList());
    }
  }

  private ImmutableSortedMap<String, Entry> getKeyEntryMap(ImmutableList<Entry> entriesInLevel) {
    SortedMap<String, Entry> keyEntryMap = new TreeMap<>();
    for (var entry : entriesInLevel) {
      Entry prevEntry = keyEntryMap.get(entry.key());
      if (prevEntry == null) {
        keyEntryMap.put(entry.key(), entry);
      } else if (prevEntry.creationEpochSeconds() < entry.creationEpochSeconds()) {
        keyEntryMap.put(entry.key(), entry);
      }
    }
    return ImmutableSortedMap.copyOfSorted(keyEntryMap);
  }
}
