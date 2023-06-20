package dev.sbutler.bitflask.storage.lsm.segment;

import static com.google.common.collect.ImmutableListMultimap.toImmutableListMultimap;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.function.Function.identity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.exceptions.StorageLoadException;
import jakarta.inject.Inject;

public final class SegmentLevelMultiMapLoader {

  private final StorageConfigurations configurations;
  private final SegmentLoader segmentLoader;
  private final SegmentIndexLoader segmentIndexLoader;

  @Inject
  SegmentLevelMultiMapLoader(
      StorageConfigurations configurations,
      SegmentLoader segmentLoader,
      SegmentIndexLoader segmentIndexLoader) {
    this.configurations = configurations;
    this.segmentLoader = segmentLoader;
    this.segmentIndexLoader = segmentIndexLoader;
  }

  public SegmentLevelMultiMap load() {
    return switch (configurations.getStorageLoadingMode()) {
      case TRUNCATE -> createWithTruncation();
      case LOAD -> createWithLoading();
    };
  }

  private SegmentLevelMultiMap createWithTruncation() {
    segmentIndexLoader.truncate();
    segmentLoader.truncate();
    return SegmentLevelMultiMap.builder().build();
  }

  private SegmentLevelMultiMap createWithLoading() {
    // Load SegmentIndexes
    ImmutableList<SegmentIndex> indexes = segmentIndexLoader.load();
    ImmutableMap<Integer, SegmentIndex> segmentNumberToIndexMap =
        mapIndexesBySegmentNumber(indexes);

    // Load Segments
    ImmutableList<Segment> segments = segmentLoader.loadWithIndexes(segmentNumberToIndexMap);

    return new SegmentLevelMultiMap.Builder(mapSegmentsBySegmentLevel(segments)).build();
  }

  private ImmutableListMultimap<Integer, Segment> mapSegmentsBySegmentLevel(
      ImmutableList<Segment> segments) {
    return segments.stream().collect(toImmutableListMultimap(Segment::getSegmentLevel, identity()));
  }

  private ImmutableMap<Integer, SegmentIndex> mapIndexesBySegmentNumber(
      ImmutableList<SegmentIndex> indexes) {
    return indexes.stream()
        .collect(toImmutableMap(SegmentIndex::getSegmentNumber, identity(), (i0, i1) -> {
          throw new StorageLoadException(String.format(
              "Duplicate segment number [%d] for SegmentIndexes found", i0.getSegmentNumber()));
        }));
  }
}
