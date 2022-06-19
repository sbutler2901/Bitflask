package dev.sbutler.bitflask.storage.segment.compactor;

import dev.sbutler.bitflask.storage.segment.Segment;
import java.util.List;
import java.util.function.Consumer;

public interface SegmentCompactor {

  void compactSegments();

  void setPreCompactedSegments(List<Segment> preCompactedSegments);

  void registerCompactionResultsConsumer(Consumer<List<Segment>> compactionResultsConsumer);

  void registerCompactionCompletedRunnable(Runnable compactionCompletedRunnable);

  void registerCompactionFailedConsumer(Consumer<Throwable> compactionFailedConsumer);
}
