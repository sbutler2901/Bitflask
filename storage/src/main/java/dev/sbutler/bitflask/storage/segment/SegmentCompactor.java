package dev.sbutler.bitflask.storage.segment;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Handler compacting the provided segments into new segments only keeping the latest key:value
 * pairs.
 * <p>
 * Note: A copy of the provided preCompactedSegments will be made during construction.
 */
class SegmentCompactor implements Callable<List<Segment>> {

  private final SegmentFactory segmentFactory;
  private final List<Segment> preCompactedSegments;

  SegmentCompactor(SegmentFactory segmentFactory, List<Segment> preCompactedSegments) {
    this.segmentFactory = segmentFactory;
    this.preCompactedSegments = List.copyOf(preCompactedSegments);
  }

  @Override
  public List<Segment> call() throws IOException {
    Map<String, Segment> keySegmentMap = createKeySegmentMap();
    List<Segment> compactedSegments = createCompactedSegments(keySegmentMap);
    markSegmentsCompacted();
    return compactedSegments;
  }

  private Map<String, Segment> createKeySegmentMap() {
    Map<String, Segment> keySegmentMap = new HashMap<>();
    for (Segment segment : preCompactedSegments) {
      Set<String> segmentKeys = segment.getSegmentKeys();
      for (String key : segmentKeys) {
        if (!keySegmentMap.containsKey(key)) {
          keySegmentMap.put(key, segment);
        }
      }
    }
    return keySegmentMap;
  }

  private List<Segment> createCompactedSegments(Map<String, Segment> keySegmentMap)
      throws IOException {
    List<Segment> compactedSegments = new CopyOnWriteArrayList<>();

    Segment currentCompactedSegment = segmentFactory.createSegment();
    for (Map.Entry<String, Segment> entry : keySegmentMap.entrySet()) {
      String key = entry.getKey();
      Optional<String> valueOptional = entry.getValue().read(key);
      if (valueOptional.isEmpty()) {
        throw new RuntimeException("Compaction failure: value not found while reading segment");
      }

      currentCompactedSegment.write(key, valueOptional.get());
      if (currentCompactedSegment.exceedsStorageThreshold()) {
        compactedSegments.add(0, currentCompactedSegment);
        currentCompactedSegment = segmentFactory.createSegment();
      }
    }
    compactedSegments.add(0, currentCompactedSegment);

    return compactedSegments;
  }

  private void markSegmentsCompacted() {
    for (Segment segment : preCompactedSegments) {
      segment.markCompacted();
    }
  }

}
