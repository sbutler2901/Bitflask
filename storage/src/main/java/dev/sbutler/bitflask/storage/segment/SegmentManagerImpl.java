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

  private final Compactor compactor = new Compactor();
  private final SegmentFactory segmentFactory;
  private Deque<Segment> segmentFilesDeque = new ConcurrentLinkedDeque<>();

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
  public Optional<String> read(String key) throws IOException {
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

  @Override
  public synchronized void write(String key, String value) throws IOException {
    Segment activeSegment = getActiveSegment();
    activeSegment.write(key, value);
  }

  private Segment getActiveSegment() throws IOException {
    Segment currentActiveSegment = segmentFilesDeque.getFirst();
    if (currentActiveSegment.exceedsStorageThreshold()) {
      return getNextActiveSegment();
    }
    return currentActiveSegment;
  }

  private Segment getNextActiveSegment() throws IOException {
    // blocks writing until new segment is created or compaction is completed
    if (shouldPerformCompaction()) {
      compactor.compactSegments();
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
    return segmentFilesDeque.size() >= DEFAULT_COMPACTION_THRESHOLD;
  }

  private class Compactor {

    private void compactSegments() throws IOException {
      Map<String, Segment> keySegmentMap = createKeySegmentMap();
      Deque<Segment> compactedSegments = createCompactedSegments(keySegmentMap);
      Deque<Segment> preCompactedSegments = segmentFilesDeque;
      segmentFilesDeque = compactedSegments;
      // todo: handle deletion with concurrent read active
//      deletePreCompactionSegments(preCompactedSegments);
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

    private Deque<Segment> createCompactedSegments(Map<String, Segment> keySegmentMap)
        throws IOException {
      Deque<Segment> compactedSegmentsDeque = new ConcurrentLinkedDeque<>();

      Segment currentCompactedSegment = segmentFactory.createSegment();
      for (Map.Entry<String, Segment> entry : keySegmentMap.entrySet()) {
        String key = entry.getKey();
        Optional<String> valueOptional = entry.getValue().read(key);
        if (valueOptional.isEmpty()) {
          throw new RuntimeException("Compaction failure: value not found while reading segment");
        }

        currentCompactedSegment.write(key, valueOptional.get());
        if (currentCompactedSegment.exceedsStorageThreshold()) {
          compactedSegmentsDeque.offerFirst(currentCompactedSegment);
          currentCompactedSegment = segmentFactory.createSegment();
        }
      }

      return compactedSegmentsDeque;
    }

    // todo: enabled segment deletion after compaction
//    private void deletePreCompactionSegments(Deque<Segment> preCompactedSegments) {
//      for (Segment segment : preCompactedSegments) {
//        try {
//          segment.closeAndDelete();
//        } catch (IOException e) {
//          System.err.println("Failure to close segment: " + e.getMessage());
//        }
//      }
//    }
  }

}
