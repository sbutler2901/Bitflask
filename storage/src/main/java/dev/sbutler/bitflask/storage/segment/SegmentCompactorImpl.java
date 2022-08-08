package dev.sbutler.bitflask.storage.segment;

import static com.google.common.util.concurrent.Futures.immediateFuture;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import dev.sbutler.bitflask.storage.configuration.concurrency.StorageExecutorService;
import dev.sbutler.bitflask.storage.segment.SegmentCompactor.CompactionResults.Failed;
import dev.sbutler.bitflask.storage.segment.SegmentCompactor.CompactionResults.Success;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;

final class SegmentCompactorImpl implements SegmentCompactor {

  private final ListeningExecutorService executorService;
  private final SegmentFactory segmentFactory;
  private final ImmutableList<Segment> segmentsToBeCompacted;

  private ListenableFuture<CompactionResults> compactionFuture = null;
  private ImmutableList<Segment> failedCompactedSegments = ImmutableList.of();

  @Inject
  SegmentCompactorImpl(@StorageExecutorService ListeningExecutorService executorService,
      SegmentFactory segmentFactory,
      @Assisted ImmutableList<Segment> segmentsToBeCompacted) {
    this.executorService = executorService;
    this.segmentFactory = segmentFactory;
    this.segmentsToBeCompacted = segmentsToBeCompacted;
  }

  @SuppressWarnings("UnstableApiUsage")
  @Override
  public synchronized ListenableFuture<CompactionResults> compactSegments() {
    if (compactionFuture != null) {
      return compactionFuture;
    }

    return compactionFuture =
        FluentFuture.from(Futures.submit(this::createKeySegmentMap, executorService))
            .transformAsync(this::createCompactedSegments, executorService)
            .transform(this::createSuccessfulCompactionResults, executorService)
            .catching(Throwable.class, this::createFailedCompactionResults, executorService);
  }

  /**
   * Creates a map of keys to the segment with the most up-to-date value for key.
   *
   * @return a map of keys to the Segment from which its corresponding value should be read
   */
  private ImmutableMap<String, Segment> createKeySegmentMap() {
    Map<String, Segment> keySegmentMap = new HashMap<>();
    for (Segment segment : segmentsToBeCompacted) {
      Set<String> segmentKeys = segment.getSegmentKeys();
      for (String key : segmentKeys) {
        if (!keySegmentMap.containsKey(key)) {
          keySegmentMap.put(key, segment);
        }
      }
    }
    return ImmutableMap.copyOf(keySegmentMap);
  }

  /**
   * Creates compacted segments using the most up-to-date value for each key.
   *
   * @param keySegmentMap a map of keys to the Segment from which its corresponding value should be
   *                      read
   * @return all compacted segments created
   */
  @Nonnull
  private ListenableFuture<ImmutableList<Segment>> createCompactedSegments(
      ImmutableMap<String, Segment> keySegmentMap) throws IOException {
    List<Segment> compactedSegments = new ArrayList<>();
    try {
      compactedSegments.add(0, segmentFactory.createSegment());
      for (Map.Entry<String, Segment> entry : keySegmentMap.entrySet()) {
        String key = entry.getKey();
        Optional<String> valueOptional = entry.getValue().read(key);
        if (valueOptional.isEmpty()) {
          throw new RuntimeException("Compaction failure: value not found while reading segment");
        }

        if (compactedSegments.get(0).exceedsStorageThreshold()) {
          compactedSegments.add(0, segmentFactory.createSegment());
        }

        compactedSegments.get(0).write(key, valueOptional.get());
      }
    } catch (IOException e) {
      failedCompactedSegments = ImmutableList.copyOf(compactedSegments);
      throw e;
    }

    return immediateFuture(ImmutableList.copyOf(compactedSegments));
  }

  private CompactionResults createSuccessfulCompactionResults(
      ImmutableList<Segment> compactedSegments) {
    markSegmentsCompacted();
    return new Success(segmentsToBeCompacted, compactedSegments);
  }

  private CompactionResults createFailedCompactionResults(Throwable throwable) {
    return new Failed(segmentsToBeCompacted, throwable, failedCompactedSegments);
  }

  private void markSegmentsCompacted() {
    for (Segment segment : segmentsToBeCompacted) {
      segment.markCompacted();
    }
  }
}
