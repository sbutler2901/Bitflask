package dev.sbutler.bitflask.storage.segment;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.function.Consumer;

/**
 * Asynchronously deletes multiple segments Handles the process of deleting multiple Segments from
 * the file system.
 * <p>
 * A callback can be registered to consume the result of deletion.
 */
interface SegmentDeleter {

  /**
   * Starts the deletion process for all Segments provided.
   */
  void deleteSegments();

  /**
   * Registers a consumer of deletion results that is called once deletion is completed
   * successfully, or because of a failure.
   *
   * @param deletionResultsConsumer the consumer to be called with the deletion results.
   */
  void registerDeletionResultsConsumer(Consumer<DeletionResults> deletionResultsConsumer);

  /**
   * Relays the results of deleting the Segments.
   */
  interface DeletionResults {

    /**
     * Used to indicate the status of executing the SegmentDeleter.
     */
    enum Status {
      SUCCESS,
      FAILED_GENERAL,
      FAILED_SEGMENTS
    }

    /**
     * The status of the deletion execution.
     */
    Status getStatus();

    /**
     * The segments that were provided to be deleted. Their state will depend on the success /
     * failure of deletion.
     *
     * @return the segments provided for deletion
     */
    ImmutableList<Segment> getSegmentsProvidedForDeletion();

    /**
     * The reason for a general failure to delete the provided segments. Will be populated when the
     * status is also set to FAILED_GENERAL.
     *
     * @return the reason for general failure
     */
    Throwable getGeneralFailureReason();

    /**
     * A map of specific segments and why they could not be successfully deleted. Will be populated
     * when the status is also set to FAILED_SEGMENTS.
     *
     * @return the map of segment to failure reasons
     */
    ImmutableMap<Segment, Throwable> getSegmentsFailureReasonsMap();
  }
}
