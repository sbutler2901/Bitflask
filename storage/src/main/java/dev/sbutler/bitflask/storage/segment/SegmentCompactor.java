package dev.sbutler.bitflask.storage.segment;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Asynchronously compacts multiple segments by de-duplicating key:value pairs and create new
 * segments to store the deduplicate pairs. Assumes Segments are in order from most recently written
 * to the earliest written.
 */
interface SegmentCompactor {

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
  ListenableFuture<CompactionResults> compactSegments();

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
}
