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
   * Returns the number of bytes of all {@link Segment}s contained within a level.
   */
  public int getSizeOfSegmentLevel(int segmentLevel) {
    return getSegmentsInLevel(segmentLevel).stream()
        .map(Segment::getSize)
        .mapToInt(Integer::intValue)
        .sum();
  }

  /**
   * Creates a new {@link Builder} populated with all entries contained within this.
   */
  public Builder toBuilder() {
    return new Builder(segmentLevelMultiMap);
  }

  public static Builder builder() {
    return new Builder();
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
     * Adds the {@link Segment} to the {@link SegmentLevelMultiMap}.
     */
    public Builder add(Segment segment) {
      segmentLevelMultiMapBuilder.put(segment.getSegmentLevel(), segment);
      return this;
    }

    /**
     * Puts all entries of the {@link com.google.common.collect.Multimap} into the builder.
     */
    public Builder addAll(Iterable<Segment> segments) {
      segments.forEach(this::add);
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
