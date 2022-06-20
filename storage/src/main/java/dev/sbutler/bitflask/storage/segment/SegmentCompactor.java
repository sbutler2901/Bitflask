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
   * Registers a consumer of compaction results that is called once compaction is completed.
   *
   * @param compactionCompletedConsumer the consumer to be called with the compaction results.
   */
  void registerCompactionCompletedConsumer(
      Consumer<CompactionCompletionResults> compactionCompletedConsumer);

  /**
   * Registers a consumer of the error that caused compaction to fail.
   *
   * @param compactionFailedConsumer the consumer to be called with the compaction failure
   */
  void registerCompactionFailedConsumer(Consumer<Throwable> compactionFailedConsumer);

  /**
   * Used to transfer the results of a successful compaction execution.
   */
  interface CompactionCompletionResults {

    /**
     * Provides the compacted segments resulting from running compaction.
     *
     * @return the compacted segments
     */
    List<Segment> compactedSegments();

    /**
     * Provides the pre-compacted segments used by the compactor during the compaction process.
     *
     * @return the pre-compacted segments
     */
    List<Segment> preCompactionSegments();
  }

}
