package dev.sbutler.bitflask.storage.segment;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import dev.sbutler.bitflask.storage.segment.SegmentCompactor.CompactionResults.Failed;
import dev.sbutler.bitflask.storage.segment.SegmentCompactor.CompactionResults.Success;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import javax.inject.Inject;

/**
 * Asynchronously compacts multiple segments by de-duplicating key:value pairs and create new
 * segments to store the deduplicate pairs. Assumes Segments are in order from most recently written
 * to the earliest written.
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

    @Inject
    Factory(SegmentFactory segmentFactory) {
      this.segmentFactory = segmentFactory;
    }

    /**
     * Creates a SegmentCompactor for compacting the provided segments. The provided Segments should
     * be in order from most recently written to the earliest written.
     *
     * @param segmentsToBeCompacted the segments that should be compacted by the created instance
     * @return the created SegmentCompactor
     */
    SegmentCompactor create(ImmutableList<Segment> segmentsToBeCompacted) {
      return new SegmentCompactor(segmentFactory, segmentsToBeCompacted);
    }
  }

  private final SegmentFactory segmentFactory;
  private final ImmutableList<Segment> segmentsToBeCompacted;

  private final AtomicBoolean compactionStarted = new AtomicBoolean();

  private SegmentCompactor(SegmentFactory segmentFactory,
      ImmutableList<Segment> segmentsToBeCompacted) {
    this.segmentFactory = segmentFactory;
    this.segmentsToBeCompacted = segmentsToBeCompacted;
  }

  /**
   * Starts the compaction process for all Segments provided.
   *
   * <p>The compaction process can only be started once. After the initial call, subsequent calls
   * will return the same ListenableFuture as the initial.
   *
   * <p>Any exceptions thrown during execution will be captured and provided in the returned
   * CompactionResults.
   *
   * @return a Future that will be fulfilled with the results of compaction, whether successful or
   * failed
   */
  public CompactionResults compactSegments() {
    if (compactionStarted.getAndSet(true)) {
      throw new IllegalStateException("Compaction has already been started");
    }

    ImmutableMap<String, Segment> keySegmentMap = createKeySegmentMap();
    return compactSegments(keySegmentMap);
  }

  /**
   * Creates a map of keys to the segment with the most up-to-date value for key.
   *
   * @return a map of keys to the Segment from which its corresponding value should be read
   */
  private ImmutableMap<String, Segment> createKeySegmentMap() {
    Map<String, Segment> keySegmentMap = new HashMap<>();
    for (Segment segment : segmentsToBeCompacted) {
      Set<String> segmentKeys = segment.getSegmentKeys();
      for (String key : segmentKeys) {
        if (!keySegmentMap.containsKey(key)) {
          keySegmentMap.put(key, segment);
        }
      }
    }
    return ImmutableMap.copyOf(keySegmentMap);
  }

  /**
   * Creates compacted segments using the most up-to-date value for each key.
   *
   * @param keySegmentMap a map of keys to the Segment from which its corresponding value should be
   *                      read
   * @return all compacted segments created
   */
  @Nonnull
  private CompactionResults compactSegments(ImmutableMap<String, Segment> keySegmentMap) {
    List<Segment> compactedSegments = new ArrayList<>();
    try {
      compactedSegments.add(0, segmentFactory.createSegment());
      for (Map.Entry<String, Segment> entry : keySegmentMap.entrySet()) {
        String key = entry.getKey();
        Optional<String> valueOptional = entry.getValue().read(key);
        if (valueOptional.isEmpty()) {
          throw new RuntimeException("Compaction failure: value not found while reading segment");
        }

        if (compactedSegments.get(0).exceedsStorageThreshold()) {
          compactedSegments.add(0, segmentFactory.createSegment());
        }

        compactedSegments.get(0).write(key, valueOptional.get());
      }
      return createSuccessfulCompactionResults(ImmutableList.copyOf(compactedSegments));
    } catch (IOException e) {
      return createFailedCompactionResults(e, ImmutableList.copyOf(compactedSegments));
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
