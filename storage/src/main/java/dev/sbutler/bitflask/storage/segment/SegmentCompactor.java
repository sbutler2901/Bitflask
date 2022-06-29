package dev.sbutler.bitflask.storage.segment;

import com.google.common.collect.ImmutableList;
import java.util.function.Consumer;

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
   * Registers a consumer of compaction results that is called onc compaction is completed
   * successfully, or because of a failure.
   *
   * @param compactionResultsConsumer the consumer to be called with the compaction results
   */
  void registerCompactionResultsConsumer(Consumer<CompactionResults> compactionResultsConsumer);

  /**
   * Used to transfer the results of a successful compaction execution.
   */
  interface CompactionResults {

    /**
     * Used to indicate the status of executing the SegmentCompactor.
     */
    enum Status {
      SUCCESS,
      FAILED
    }

    /**
     * The status of the compaction execution.
     *
     * @return compaction execution status
     */
    Status getStatus();

    /**
     * Provides the segments that were used by the compactor during the compaction process.
     *
     * @return the segments provided for compaction.
     */
    ImmutableList<Segment> getSegmentsProvidedForCompaction();

    /**
     * Provides the compacted segments resulting from running compaction. Will be populated when the
     * status is also set to SUCCESS.
     *
     * @return the compacted segments
     */
    ImmutableList<Segment> getCompactedSegments();

    /**
     * The reason for failure during compaction execution. Will be populated when the status is also
     * set to FAILED.
     *
     * @return the reason for failure
     */
    Throwable getFailureReason();

    /**
     * Any segments created during execution. The segments should not be considered complete and
     * valid for usage. Will be populated when the status is also set to FAILED.
     *
     * @return any segments creating during failed compaction execution
     */
    ImmutableList<Segment> getFailedCompactedSegments();

  }

}
