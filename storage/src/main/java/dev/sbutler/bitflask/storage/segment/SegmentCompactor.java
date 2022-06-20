package dev.sbutler.bitflask.storage.segment;

import java.util.List;
import java.util.function.Consumer;

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
