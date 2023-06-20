package dev.sbutler.bitflask.storage.lsm;

import dev.sbutler.bitflask.storage.exceptions.StorageReadException;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import dev.sbutler.bitflask.storage.lsm.segment.Segment;
import dev.sbutler.bitflask.storage.lsm.segment.SegmentLevelMultiMap;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import jdk.incubator.concurrent.StructuredTaskScope;

/** Handles read related tasks for the {@link LSMTree}. */
final class LSMTreeReader {

  private final LSMTreeStateManager stateManager;
  private final ThreadFactory threadFactory;

  @Inject
  LSMTreeReader(LSMTreeStateManager stateManager, ThreadFactory threadFactory) {
    this.stateManager = stateManager;
    this.threadFactory = threadFactory;
  }

  /**
   * Reads the {@link dev.sbutler.bitflask.storage.lsm.entry.Entry} associated with the key and
   * returns it, if present.
   */
  Optional<Entry> read(String key) {
    try (var currentState = stateManager.getCurrentState()) {
      return currentState
          .getMemtable()
          .read(key)
          .or(() -> readFromSegments(currentState.getSegmentLevelMultiMap(), key));
    }
  }

  private Optional<Entry> readFromSegments(SegmentLevelMultiMap segmentLevelMultiMap, String key) {
    for (var segmentLevel : segmentLevelMultiMap.getSegmentLevels()) {
      Optional<Entry> minEntryValue =
          readMinEntryAtSegmentLevel(segmentLevelMultiMap, key, segmentLevel);
      if (minEntryValue.isPresent()) {
        return minEntryValue;
      }
    }
    return Optional.empty();
  }

  private Optional<Entry> readMinEntryAtSegmentLevel(
      SegmentLevelMultiMap segmentLevelMultiMap, String key, int segmentLevel) {
    try (var scope =
        new StructuredTaskScope.ShutdownOnFailure("read-segments-scope", threadFactory)) {
      List<Future<Optional<Entry>>> segmentReadFutures = new ArrayList<>();
      for (Segment segment : segmentLevelMultiMap.getSegmentsInLevel(segmentLevel)) {
        if (segment.mightContain(key)) {
          segmentReadFutures.add(scope.fork(() -> segment.readEntry(key)));
        }
      }
      if (segmentReadFutures.isEmpty()) {
        return Optional.empty();
      }

      try {
        scope.join();
        scope.throwIfFailed(StorageReadException::new);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new StorageReadException(e);
      }

      return segmentReadFutures.stream()
          .map(Future::resultNow)
          .flatMap(Optional::stream)
          .min(Comparator.comparingLong(Entry::creationEpochSeconds));
    }
  }
}
