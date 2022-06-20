package dev.sbutler.bitflask.storage.segment;

import java.util.List;
import java.util.function.Consumer;

/**
 * Asynchronously compacts multiple segments by de-duplicating key:value pairs and create new
 * segments to store the deduplicate pairs. The provided segments will be deleted after all have
 * been successfully compacted.
 * <p>
 * Callbacks can be registered to be executed after the various stages of compaction complete.
 */
interface SegmentCompactor {

  /**
   * Initiates the compaction process. This should only be called once the segments to be compacted
   * have been set.
   */
  void compactSegments();

  /**
   * Registers a consumer of the compacted segments.
   *
   * @param compactionResultsConsumer the consumer to be called with the compacted segments
   */
  void registerCompactedSegmentsConsumer(Consumer<List<Segment>> compactionResultsConsumer);

  /**
   * Registers a runnable to be executed after compaction has completed.
   *
   * @param compactionCompletedRunnable the runnable to be called after compaction completion
   */
  void registerCompactionCompletedRunnable(Runnable compactionCompletedRunnable);

  /**
   * Registers a consumer of the error that caused compaction to fail.
   *
   * @param compactionFailedConsumer the consumer to be called with the compaction failure
   */
  void registerCompactionFailedConsumer(Consumer<Throwable> compactionFailedConsumer);
}
