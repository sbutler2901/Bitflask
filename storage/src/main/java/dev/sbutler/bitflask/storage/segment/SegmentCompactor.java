package dev.sbutler.bitflask.storage.segment;

import com.google.common.collect.ImmutableList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

// Todo: Simply consumers using a single result object like SegmentDeleter

/**
 * Asynchronously compacts multiple segments by de-duplicating key:value pairs and create new
 * segments to store the deduplicate pairs. Assumes Segments are in order from most recently written
 * to the earliest written.
 * <p>
 * Callbacks can be registered to consume the results of the compaction in the case of success or
 * failure.
 */
interface SegmentCompactor {

  /**
   * Initiates the compaction process.
   */
  void compactSegments();

  /**
   * Registers a consumer of compaction results that is called once compaction is completed
   * successfully.
   *
   * @param compactionCompletedConsumer the consumer to be called with the compaction results.
   */
  void registerCompactionCompletedConsumer(
      Consumer<CompactionCompletionResults> compactionCompletedConsumer);

  /**
   * Registers a consumer of the error that caused compaction to fail and any segments created
   * during execution. The segments should not be considered complete and valid for usage.
   *
   * @param compactionFailedConsumer the consumer to be called with the compaction failure and
   *                                 created segments
   */
  void registerCompactionFailedConsumer(
      BiConsumer<Throwable, ImmutableList<Segment>> compactionFailedConsumer);

  /**
   * Used to transfer the results of a successful compaction execution.
   */
  interface CompactionCompletionResults {

    /**
     * Provides the compacted segments resulting from running compaction.
     *
     * @return the compacted segments
     */
    ImmutableList<Segment> compactedSegments();

    /**
     * Provides the segments that were used by the compactor during the compaction process.
     *
     * @return the segments provided for compaction.
     */
    ImmutableList<Segment> segmentsProvidedForCompaction();
  }

}
