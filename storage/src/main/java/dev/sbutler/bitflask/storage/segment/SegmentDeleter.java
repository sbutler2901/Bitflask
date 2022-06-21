package dev.sbutler.bitflask.storage.segment;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

interface SegmentDeleter {

  void deleteSegments();

  /**
   * Registers a consumer of deletion results that is called once deletion is completed
   * successfully, or because of a failure.
   *
   * @param deletionResultsConsumer the consumer to be called with the deletion results.
   */
  void registerDeletionResultsConsumer(Consumer<DeletionResults> deletionResultsConsumer);

  /**
   * Used to indicate the status of executing the SegmentDeleter.
   */
  interface DeletionResults {

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
    List<Segment> getSegmentsToBeDeleted();

    /**
     * The reason for a general failure to delete the provided segments. Will be populated when the
     * status is also set to FAILED_GENERAL.
     *
     * @return the reason for general failure
     */
    Optional<Throwable> getGeneralFailureReason();

    /**
     * A map of specific segments and why they could not be successfully deleted. Will be populated
     * when the status is also set to FAILED_SEGMENTS.
     *
     * @return the map of segment to failure reasons
     */
    Optional<Map<Segment, Throwable>> getSegmentsFailureReasonsMap();
  }
}
