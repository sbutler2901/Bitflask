package dev.sbutler.bitflask.storage.segment;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

class SegmentManagerImpl implements SegmentManager {

  private static final int DEFAULT_COMPACTION_THRESHOLD = 2;

  private final SegmentFactory segmentFactory;
  private final Deque<Segment> segmentFilesDeque = new ConcurrentLinkedDeque<>();

  @Inject
  SegmentManagerImpl(SegmentFactory segmentFactory) throws IOException {
    this.segmentFactory = segmentFactory;
    initializeSegments();
  }

  // todo: create segment loader for pre-existing segments
  public void initializeSegments() throws IOException {
    // todo: handle detecting previous segments after compaction
    boolean loadNextSegment = true;
    while (loadNextSegment) {
      Segment nextSegment = createAndAddNextActiveSegment();
      loadNextSegment = nextSegment.exceedsStorageThreshold();
    }
  }

  @Override
  public synchronized void write(String key, String value) throws IOException {
    Segment activeSegment = getActiveSegment();
    activeSegment.write(key, value);
  }

  @Override
  public Optional<String> read(String key) {
    Optional<Segment> optionalSegment = findLatestSegmentWithKey(key);
    if (optionalSegment.isEmpty()) {
      return Optional.empty();
    }
    return optionalSegment.get().read(key);
  }

  private Optional<Segment> findLatestSegmentWithKey(String key) {
    for (Segment segment : segmentFilesDeque) {
      if (segment.containsKey(key)) {
        return Optional.of(segment);
      }
    }
    return Optional.empty();
  }

  private synchronized Segment getActiveSegment() throws IOException {
    Segment currentActiveSegment = segmentFilesDeque.getFirst();
    if (currentActiveSegment.exceedsStorageThreshold()) {
      return getNextActiveSegment();
    }
    return currentActiveSegment;
  }

  private synchronized Segment getNextActiveSegment() throws IOException {
    // blocks writing until new segment is created or compaction is completed
    if (shouldPerformCompaction()) {
      compactSegments();
    } else {
      createAndAddNextActiveSegment();
    }
    return segmentFilesDeque.getFirst();
  }

  private Segment createAndAddNextActiveSegment() throws IOException {
    Segment segment = segmentFactory.createSegment();
    segmentFilesDeque.offerFirst(segment);
    return segment;
  }

  private boolean shouldPerformCompaction() {
    return segmentFilesDeque.size() % DEFAULT_COMPACTION_THRESHOLD == 0;
  }

  private synchronized void compactSegments() throws IOException {
    Map<String, Segment> keySegmentMap = createKeySegmentMap();
    Segment compactedSegment = createCompactedSegment(keySegmentMap);
    deleteCompactedSegments();
    finalizeCompaction(compactedSegment);
  }

  private Map<String, Segment> createKeySegmentMap() {
    Map<String, Segment> keySegmentMap = new HashMap<>();
    for (Segment segment : segmentFilesDeque) {
      Set<String> segmentKeys = segment.getSegmentKeys();
      for (String key : segmentKeys) {
        if (!keySegmentMap.containsKey(key)) {
          keySegmentMap.put(key, segment);
        }
      }
    }
    return keySegmentMap;
  }

  private Segment createCompactedSegment(Map<String, Segment> keySegmentMap) throws IOException {
    Segment compactedSegment = segmentFactory.createSegment();

    for (Map.Entry<String, Segment> entry : keySegmentMap.entrySet()) {
      String key = entry.getKey();
      Optional<String> valueOptional = entry.getValue().read(key);
      if (valueOptional.isEmpty()) {
        throw new RuntimeException("Compaction failure: value not found while reading segment");
      }
      compactedSegment.write(key, valueOptional.get());
    }

    return compactedSegment;
  }


  private void deleteCompactedSegments() {
    for (Segment segment : segmentFilesDeque) {
      try {
        segment.closeAndDelete();
      } catch (IOException e) {
        System.err.println("Failure to close segment: " + e.getMessage());
      }
    }
  }

  private void finalizeCompaction(Segment compactedSegment) {
    segmentFilesDeque.clear();
    segmentFilesDeque.offerFirst(compactedSegment);
  }

}
