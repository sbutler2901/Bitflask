package dev.sbutler.bitflask.storage.lsm;

import dev.sbutler.bitflask.storage.exceptions.StorageReadException;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import dev.sbutler.bitflask.storage.lsm.memtable.Memtable;
import dev.sbutler.bitflask.storage.lsm.segment.Segment;
import dev.sbutler.bitflask.storage.lsm.segment.SegmentLevelMultiMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import javax.inject.Inject;
import javax.inject.Provider;
import jdk.incubator.concurrent.StructuredTaskScope;

/**
 * Handles read related tasks for the {@link LSMTree}.
 */
final class LSMTreeReader {

  private final ThreadFactory threadFactory;
  private final Provider<Memtable> memtableProvider;
  private final Provider<SegmentLevelMultiMap> segmentLevelMultiMapProvider;

  @Inject
  LSMTreeReader(ThreadFactory threadFactory,
      Provider<Memtable> memtableProvider,
      Provider<SegmentLevelMultiMap> segmentLevelMultiMapProvider) {
    this.threadFactory = threadFactory;
    this.memtableProvider = memtableProvider;
    this.segmentLevelMultiMapProvider = segmentLevelMultiMapProvider;
  }

  Optional<Entry> read(String key) {
    return memtableProvider.get().read(key)
        .or(() -> readFromSegments(key));
  }

  private Optional<Entry> readFromSegments(String key) {
    SegmentLevelMultiMap segmentLevelMultiMap = segmentLevelMultiMapProvider.get();
    for (var segmentLevel : segmentLevelMultiMap.getSegmentLevels()) {
      Optional<Entry> minEntryValue =
          readMinEntryValueFromSegmentLevel(segmentLevelMultiMap, key, segmentLevel);
      if (minEntryValue.isPresent()) {
        return minEntryValue;
      }
    }
    return Optional.empty();
  }

  private Optional<Entry> readMinEntryValueFromSegmentLevel(
      SegmentLevelMultiMap segmentLevelMultiMap,
      String key, int segmentLevel) {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure(
        "read-segments-scope", threadFactory)) {
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
        scope.throwIfFailed();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new StorageReadException(e);
      } catch (ExecutionException e) {
        throw new StorageReadException(e);
      }

      return segmentReadFutures.stream()
          .map(Future::resultNow)
          .flatMap(Optional::stream)
          .min(Comparator.comparingLong(Entry::creationEpochSeconds));
    }
  }
}
