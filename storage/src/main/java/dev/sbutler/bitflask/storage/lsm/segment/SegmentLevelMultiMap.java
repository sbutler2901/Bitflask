package dev.sbutler.bitflask.storage.lsm.segment;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

/**
 * A {@link com.google.common.collect.Multimap} of segment levels to {@link Segment}s currently in
 * that level.
 *
 * <p>Lower segment levels represent newer Segments.
 */
public final class SegmentLevelMultiMap {

  private final ImmutableListMultimap<Integer, Segment> segmentLevelMultiMap;

  private SegmentLevelMultiMap(ImmutableListMultimap<Integer, Segment> segmentLevelMultiMap) {
    this.segmentLevelMultiMap = segmentLevelMultiMap;
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

  /**
   * Creates a new {@link Builder} populated with all entries contained within this.
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  /**
   * Returns the underlying {@link com.google.common.collect.Multimap} supporting this.
   */
  private Multimap<Integer, Segment> flush() {
    return segmentLevelMultiMap;
  }

  /**
   * A builder class for creating {@link SegmentLevelMultiMap} instances.
   */
  public static class Builder {

    private final ImmutableListMultimap.Builder<Integer, Segment> segmentLevelMultiMapBuilder;

    /**
     * Initializes an empty {@link Builder}.
     */
    public Builder() {
      segmentLevelMultiMapBuilder = ImmutableListMultimap.<Integer, Segment>builder()
          .orderKeysBy(Integer::compare);
    }

    /**
     * Initializes a {@link Builder} with the provided {@link Multimap}'s entries.
     */
    public Builder(Multimap<Integer, Segment> segmentLevelMultiMap) {
      this();
      segmentLevelMultiMapBuilder.putAll(segmentLevelMultiMap);
    }

    /**
     * Initializes a {@link Builder} with the provided {@link SegmentLevelMultiMap}'s entries.
     */
    public Builder(SegmentLevelMultiMap segmentLevelMultiMap) {
      this();
      segmentLevelMultiMapBuilder.putAll(segmentLevelMultiMap.flush());
    }

    /**
     * Puts the Segment at the specified level.
     */
    public Builder put(int segmentLevel, Segment segment) {
      segmentLevelMultiMapBuilder.put(segmentLevel, segment);
      return this;
    }

    /**
     * Puts all entries of the {@link com.google.common.collect.Multimap} into the builder.
     */
    public Builder putAll(Multimap<Integer, Segment> segmentLevelMultiMap) {
      segmentLevelMultiMapBuilder.putAll(segmentLevelMultiMap);
      return this;
    }

    /**
     * Creates a new {@link SegmentLevelMultiMap} with the values provided to this {@link Builder}.
     */
    public SegmentLevelMultiMap build() {
      return new SegmentLevelMultiMap(segmentLevelMultiMapBuilder.build());
    }
  }
}
