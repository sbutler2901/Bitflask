package dev.sbutler.bitflask.storage.segmentV1;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import dev.sbutler.bitflask.storage.segmentV1.Encoder.Header;
import dev.sbutler.bitflask.storage.segmentV1.SegmentCompactor.CompactionResults.Failed;
import dev.sbutler.bitflask.storage.segmentV1.SegmentCompactor.CompactionResults.Success;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import jdk.incubator.concurrent.StructuredTaskScope;

/**
 * Compacts multiple segments by de-duplicating key:value pairs and create new segments to store the
 * deduplicate pairs. Assumes Segments are in order from most recently written to the earliest
 * written.
 */
final class SegmentCompactor {

  /**
   * Relays the results of a compaction execution.
   */
  sealed interface CompactionResults {

    /**
     * Contains the results of a successful compaction execution
     */
    record Success(ImmutableList<Segment> segmentsProvidedForCompaction,
                   ImmutableList<Segment> compactedSegments) implements CompactionResults {

    }

    /**
     * Contains the results of a failed compaction execution
     */
    record Failed(ImmutableList<Segment> segmentsProvidedForCompaction, Throwable failureReason,
                  ImmutableList<Segment> failedCompactionSegments) implements CompactionResults {

    }
  }

  /**
   * Factory for creating new SegmentCompactor instances.
   */
  static class Factory {

    private final SegmentFactory segmentFactory;
    private final ThreadFactory threadFactory;

    @Inject
    Factory(SegmentFactory segmentFactory, ThreadFactory threadFactory) {
      this.segmentFactory = segmentFactory;
      this.threadFactory = threadFactory;
    }

    /**
     * Creates a SegmentCompactor for compacting the provided segments. The provided Segments should
     * be in order from most recently written to the earliest written.
     *
     * @param segmentsToBeCompacted the segments that should be compacted by the created instance
     * @return the created SegmentCompactor
     */
    SegmentCompactor create(ImmutableList<Segment> segmentsToBeCompacted) {
      return new SegmentCompactor(segmentFactory, threadFactory, segmentsToBeCompacted);
    }
  }

  private final SegmentFactory segmentFactory;
  private final ThreadFactory threadFactory;
  private final ImmutableList<Segment> segmentsToBeCompacted;

  private final AtomicBoolean compactionStarted = new AtomicBoolean();

  private SegmentCompactor(SegmentFactory segmentFactory,
      ThreadFactory threadFactory,
      ImmutableList<Segment> segmentsToBeCompacted) {
    this.segmentFactory = segmentFactory;
    this.threadFactory = threadFactory;
    this.segmentsToBeCompacted = segmentsToBeCompacted;
  }

  /**
   * Starts the compaction process for all Segments provided.
   *
   * <p>This is a blocking call.
   *
   * <p>The compaction process can only be started once. Subsequent calls will result in an
   * IllegalStateException being thrown.
   *
   * <p>Anticipated exceptions thrown during execution will be captured and provided in the
   * returned CompactionResults.
   */
  public CompactionResults compactSegments() {
    if (compactionStarted.getAndSet(true)) {
      throw new IllegalStateException("Compaction has already been started");
    }

    ImmutableMap<String, Segment> keySegmentMap = createKeySegmentMap();
    ImmutableMap<String, String> keyValueMap;
    try {
      keyValueMap = createKeyValueMap(keySegmentMap);
    } catch (InterruptedException | ExecutionException e) {
      return createFailedCompactionResults(e, ImmutableList.of());
    }

    return compactSegments(keyValueMap);
  }

  /**
   * Creates a map of keys to the segment with the most up-to-date value for key.
   *
   * <p>The header of a key the first time it is encountered will determine if it is added to the
   * map or skipped. If the key is still active a map entry will be created to the most recent
   * containing it. If the key is deleted it will be marked and ignored even if encountered in older
   * segments.
   */
  private ImmutableMap<String, Segment> createKeySegmentMap() {
    Map<String, Segment> keySegmentMap = new HashMap<>();
    Set<String> deletedKeys = new HashSet<>();

    for (Segment segment : segmentsToBeCompacted) {
      ImmutableMap<String, Header> segmentKeyHeaderMap =
          segment.getSegmentKeyHeaderMap();
      for (Map.Entry<String, Header> keyHeaderEntry : segmentKeyHeaderMap.entrySet()) {
        String key = keyHeaderEntry.getKey();
        Header header = keyHeaderEntry.getValue();
        // skip keys already seen
        if (keySegmentMap.containsKey(key) || deletedKeys.contains(key)) {
          continue;
        }
        // Only add keys to map that are not deleted
        if (header.equals(Header.DELETED)) {
          deletedKeys.add(key);
        } else {
          keySegmentMap.put(key, segment);
        }
      }
    }
    return ImmutableMap.copyOf(keySegmentMap);
  }

  /**
   * Creates a key value map by reading the value of a key from the mapped Segment.
   */
  private ImmutableMap<String, String> createKeyValueMap(
      ImmutableMap<String, Segment> keySegmentMap) throws InterruptedException, ExecutionException {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure("compaction-key-value-map-scope",
        threadFactory)) {
      ImmutableList<Future<Entry<String, String>>> taskFutures =
          keySegmentMap.entrySet().stream()
              .map(this::createKeyValueCallable)
              .map(scope::fork)
              .collect(toImmutableList());
      scope.join();
      scope.throwIfFailed();

      return taskFutures.stream().map(Future::resultNow)
          .collect(toImmutableMap(Entry::getKey, Entry::getValue));
    }
  }

  /**
   * Creates a Callable that reads a key's value from its mapped Segment.
   */
  private Callable<Entry<String, String>> createKeyValueCallable(
      Map.Entry<String, Segment> keySegmentEntry) {
    return () -> {
      String key = keySegmentEntry.getKey();
      Segment segment = keySegmentEntry.getValue();
      String value = segment.read(key).orElseThrow(
          () -> new RuntimeException(
              "Compaction failure: value not found while reading key [%s] from segment [%s]".formatted(
                  key, segment.getSegmentFileKey())));
      return Maps.immutableEntry(key, value);
    };
  }

  /**
   * Creates the compacted segments using the provided key:value map.
   */
  private CompactionResults compactSegments(ImmutableMap<String, String> keyValueMap) {
    Deque<Segment> compactedSegments = new ArrayDeque<>();
    try {
      compactedSegments.offerFirst(segmentFactory.createSegment());
      for (Map.Entry<String, String> entry : keyValueMap.entrySet()) {
        if (compactedSegments.getFirst().exceedsStorageThreshold()) {
          compactedSegments.offerFirst(segmentFactory.createSegment());
        }
        compactedSegments.getFirst().write(entry.getKey(), entry.getValue());
      }
      return createSuccessfulCompactionResults(
          compactedSegments.stream().collect(toImmutableList()));
    } catch (IOException e) {
      return createFailedCompactionResults(e,
          compactedSegments.stream().collect(toImmutableList()));
    }
  }

  private CompactionResults createSuccessfulCompactionResults(
      ImmutableList<Segment> compactedSegments) {
    markSegmentsCompacted();
    return new Success(segmentsToBeCompacted, compactedSegments);
  }

  private CompactionResults createFailedCompactionResults(Throwable throwable,
      ImmutableList<Segment> failedCompactedSegments) {
    return new Failed(segmentsToBeCompacted, throwable, failedCompactedSegments);
  }

  private void markSegmentsCompacted() {
    for (Segment segment : segmentsToBeCompacted) {
      segment.markCompacted();
    }
  }
}
