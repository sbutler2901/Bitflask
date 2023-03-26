package dev.sbutler.bitflask.storage.lsm.segment;

import com.google.common.collect.ImmutableSortedMap;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import java.nio.file.Path;
import java.util.Optional;

/**
 * A dense index implementation of {@link SegmentIndex}.
 *
 * <p>Every {@link Entry} in a {@link Segment} will have its
 * key mapped to its corresponding file offset.
 */
final class SegmentIndexDense implements SegmentIndex {

  private final Path filePath;
  private final SegmentIndexMetadata metadata;
  private final ImmutableSortedMap<String, Long> keyOffsetMap;

  SegmentIndexDense(
      Path filePath,
      SegmentIndexMetadata metadata,
      ImmutableSortedMap<String, Long> keyOffsetMap) {
    this.filePath = filePath;
    this.metadata = metadata;
    this.keyOffsetMap = keyOffsetMap;
  }

  public boolean mightContain(String key) {
    return keyOffsetMap.containsKey(key);
  }

  public Optional<Long> getKeyOffset(String key) {
    return Optional.ofNullable(keyOffsetMap.get(key));
  }

  public int getSegmentNumber() {
    return metadata.segmentNumber().value();
  }

  public Path getFilePath() {
    return filePath;
  }
}
