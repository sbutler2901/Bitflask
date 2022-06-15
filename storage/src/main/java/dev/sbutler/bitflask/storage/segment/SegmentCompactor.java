package dev.sbutler.bitflask.storage.segment;

import java.util.List;
import java.util.function.Consumer;

interface SegmentCompactor {

  void compactSegments();

  void setPreCompactedSegments(List<Segment> preCompactedSegments);

  void registerCompactionResultsConsumer(Consumer<List<Segment>> compactionResultsConsumer);

  void registerCompactionCompletedRunnable(Runnable compactionCompletedRunnable);

  void registerCompactionFailedConsumer(Consumer<Throwable> compactionFailedConsumer);
}
