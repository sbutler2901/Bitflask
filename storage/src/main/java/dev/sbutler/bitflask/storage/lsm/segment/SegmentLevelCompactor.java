package dev.sbutler.bitflask.storage.lsm.segment;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.storage.exceptions.StorageCompactionException;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import dev.sbutler.bitflask.storage.lsm.entry.EntryUtils;
import dev.sbutler.bitflask.storage.lsm.segment.Segment.PathsForDeletion;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import javax.inject.Inject;
import jdk.incubator.concurrent.StructuredTaskScope;

/**
 * Handles compacting all {@link Segment}s in a level.
 */
public final class SegmentLevelCompactor {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ThreadFactory threadFactory;
  private final SegmentFactory segmentFactory;

  @Inject
  SegmentLevelCompactor(ThreadFactory threadFactory, SegmentFactory segmentFactory) {
    this.threadFactory = threadFactory;
    this.segmentFactory = segmentFactory;
  }

  /**
   * Compacts the provided segment level returning the updated
   * {@link dev.sbutler.bitflask.storage.lsm.segment.SegmentLevelMultiMap}.
   */
  public SegmentLevelMultiMap compactSegmentLevel(
      SegmentLevelMultiMap segmentLevelMultiMap, int segmentLevel) {
    // TODO: evaluate performance
    ImmutableList<Segment> segmentsInLevel = segmentLevelMultiMap.getSegmentsInLevel(segmentLevel);
    ImmutableList<Entry> entriesInLevel = getAllEntriesInLevel(segmentsInLevel);
    ImmutableSortedMap<String, Entry> keyEntryMap =
        EntryUtils.buildImmutableKeyEntryMap(entriesInLevel);

    Segment newSegment;
    try {
      newSegment = segmentFactory.create(keyEntryMap, segmentLevel + 1);
    } catch (IOException e) {
      throw new StorageCompactionException("Failed creating new segment", e);
    }

    deleteCompactedSegments(segmentsInLevel);

    logger.atInfo()
        .log("Compacted segment level [%d] into Segment [%d] removing [%d] duplicate Entries",
            segmentLevel,
            newSegment.getSegmentNumber(),
            entriesInLevel.size() - keyEntryMap.size());

    return segmentLevelMultiMap.toBuilder()
        .clearSegmentLevel(segmentLevel)
        .add(newSegment)
        .build();
  }

  private ImmutableList<Entry> getAllEntriesInLevel(
      ImmutableList<Segment> segmentsInLevel) {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure("compact-segments-level-scope",
        threadFactory)) {
      ImmutableList<Future<ImmutableList<Entry>>> segmentEntriesFutures =
          segmentsInLevel.stream()
              .map(segment -> scope.fork(segment::readAllEntries))
              .collect(toImmutableList());

      try {
        scope.join();
        scope.throwIfFailed(e ->
            new StorageCompactionException("Failed getting all entries in segment level", e));
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

  /**
   * Best effort deletion of all compacted {@link Segment}s and their {@link SegmentIndex}.
   */
  private void deleteCompactedSegments(ImmutableList<Segment> compactedSegments) {
    for (var segment : compactedSegments) {
      PathsForDeletion pathsForDeletion = segment.getPathsForDeletion();
      try {
        Files.delete(pathsForDeletion.segmentPath());
        logger.atInfo().log(String.format("Deleted Segment [%s]", pathsForDeletion.segmentPath()));
      } catch (IOException e) {
        logger.atSevere().withCause(e)
            .log(String.format("Failed to delete Segment [%s]", pathsForDeletion.segmentPath()));
        // Don't delete index if Segment failed to be deleted
        continue;
      }
      try {
        Files.delete(pathsForDeletion.indexPath());
        logger.atInfo()
            .log(String.format("Deleted SegmentIndex [%s]", pathsForDeletion.indexPath()));
      } catch (IOException e) {
        logger.atSevere().withCause(e)
            .log(String.format("Failed to delete SegmentIndex [%s]", pathsForDeletion.indexPath()));
      }
    }
  }
}
