package dev.sbutler.bitflask.storage.lsm.segment;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;

/**
 * A {@link com.google.common.collect.Multimap} of segment levels to {@link Segment}s currently in
 * that level.
 */
public final class SegmentLevelMultiMap {

  private final ImmutableListMultimap<Integer, Segment> segmentLevelMultiMap;

  private SegmentLevelMultiMap(ImmutableListMultimap<Integer, Segment> segmentLevelMultiMap) {
    this.segmentLevelMultiMap = segmentLevelMultiMap;
  }

  public static SegmentLevelMultiMap create() {
    return new SegmentLevelMultiMap(ImmutableListMultimap.of());
  }

  public static SegmentLevelMultiMap create(
      ImmutableListMultimap<Integer, Segment> segmentLevelMultiMap) {
    ImmutableListMultimap<Integer, Segment> sortedByKey =
        ImmutableListMultimap.<Integer, Segment>builder()
            .orderKeysBy(Integer::compare)
            .putAll(segmentLevelMultiMap)
            .build();
    return new SegmentLevelMultiMap(sortedByKey);
  }

  /**
   * Gets the contained segment levels in order from smallest to larges.
   */
  public ImmutableSet<Integer> getSegmentLevels() {
    return segmentLevelMultiMap.keySet();
  }

  /**
   * Gets all segments in the provided segment level.
   */
  public ImmutableList<Segment> getSegmentsInLevel(int segmentLevel) {
    return segmentLevelMultiMap.get(segmentLevel);
  }
}
