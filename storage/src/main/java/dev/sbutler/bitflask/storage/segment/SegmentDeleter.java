package dev.sbutler.bitflask.storage.segment;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Asynchronously deletes multiple segments Handles the process of deleting multiple Segments from
 * the file system.
 */
interface SegmentDeleter {

  /**
   * Starts the deletion process for all Segments provided.
   *
   * <p>The deletion process can only be started once. After the initial call, subsequent calls
   * will return the same ListenableFuture as the initial.
   *
   * <p>Any exceptions thrown during execution will be captured and provided in the returned
   * DeletionResults.
   *
   * @return a Future that will be fulfilled with the results of deletion, whether success or
   * failure
   */
  ListenableFuture<DeletionResults> deleteSegments();

  /**
   * Relays the results of a deletion execution
   */
  sealed interface DeletionResults {

    record Success(ImmutableList<Segment> segmentsProvidedForDeletion)
        implements DeletionResults {

    }

    /**
     * Contains the result of failed deletion execution caused by a general failure
     */
    record FailedGeneral(ImmutableList<Segment> segmentsProvidedForDeletion,
                         Throwable failureReason) implements DeletionResults {

    }

    /**
     * Contains the result of failed deletion execution caused by specific segment(s)
     */
    record FailedSegments(ImmutableList<Segment> segmentsProvidedForDeletion,
                          ImmutableMap<Segment, Throwable> segmentsFailureReasonsMap)
        implements DeletionResults {

    }
  }
}
