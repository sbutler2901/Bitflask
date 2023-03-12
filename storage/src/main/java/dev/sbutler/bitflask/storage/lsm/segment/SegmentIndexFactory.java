package dev.sbutler.bitflask.storage.lsm.segment;

import com.google.common.collect.ImmutableSortedMap;
import dev.sbutler.bitflask.common.primitives.UnsignedShort;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import javax.inject.Inject;

/**
 * Handles the creation of a {@link SegmentIndex}.
 */
final class SegmentIndexFactory {

  private final StorageConfigurations configurations;

  @Inject
  SegmentIndexFactory(StorageConfigurations configurations) {
    this.configurations = configurations;
  }

  /**
   * Creates a new {@link SegmentIndex} and writes it to disk.
   */
  SegmentIndex create(
      ImmutableSortedMap<String, Long> keyOffsetMap,
      UnsignedShort segmentNumber) throws IOException {
    SegmentIndexMetadata indexMetadata = new SegmentIndexMetadata(segmentNumber);
    ImmutableSortedMap.Builder<String, Long> indexKeyOffsetMap = ImmutableSortedMap.naturalOrder();

    Path indexPath = Path.of(configurations.getStorageStoreDirectoryPath().toString(),
        SegmentIndex.createFileName(segmentNumber.value()));

    try (BufferedOutputStream indexOutputStream = new BufferedOutputStream(
        Files.newOutputStream(indexPath, StandardOpenOption.CREATE_NEW))) {

      indexOutputStream.write(indexMetadata.getBytes());

      for (Map.Entry<String, Long> entry : keyOffsetMap.entrySet()) {
        SegmentIndexEntry indexEntry = new SegmentIndexEntry(entry.getKey(), entry.getValue());
        indexOutputStream.write(indexEntry.getBytes());
        indexKeyOffsetMap.put(indexEntry.key(), indexEntry.offset());
      }
    }

    return new SegmentIndexDense(indexMetadata, indexKeyOffsetMap.build());
  }

  /**
   * Loads a {@link SegmentIndex} from disk at the provided path.
   */
  SegmentIndex loadFromPath(Path path) throws IOException {
    // TODO: implement
    return null;
  }
}
