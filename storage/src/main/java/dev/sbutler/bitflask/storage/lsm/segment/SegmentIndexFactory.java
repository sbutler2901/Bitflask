package dev.sbutler.bitflask.storage.lsm.segment;

import com.google.common.collect.ImmutableSortedMap;
import dev.sbutler.bitflask.common.primitives.UnsignedShort;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.exceptions.StorageLoadException;
import dev.sbutler.bitflask.storage.lsm.segment.SegmentIndexEntry.PartialEntry;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Optional;
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
    try (BufferedInputStream is = new BufferedInputStream(Files.newInputStream(path))) {
      byte[] metadataBytes = is.readNBytes(SegmentIndexMetadata.BYTES);
      if (metadataBytes.length != SegmentIndexMetadata.BYTES) {
        throw new StorageLoadException(String.format(
            "SegmentIndex SegmentIndexMetadata bytes read too short. Expected [%d], actual [%d]",
            SegmentIndexMetadata.BYTES, metadataBytes.length));
      }
      SegmentIndexMetadata metadata = SegmentIndexMetadata.fromBytes(metadataBytes);

      ImmutableSortedMap.Builder<String, Long> indexKeyOffsetMap = ImmutableSortedMap.naturalOrder();

      Optional<SegmentIndexEntry> nextEntry;
      while ((nextEntry = readNextSegmentIndexEntry(is)).isPresent()) {
        indexKeyOffsetMap.put(nextEntry.get().key(), nextEntry.get().offset());
      }

      return new SegmentIndexDense(metadata, indexKeyOffsetMap.build());
    }
  }

  private Optional<SegmentIndexEntry> readNextSegmentIndexEntry(
      BufferedInputStream is) throws IOException {
    byte[] partialEntryBytes = is.readNBytes(PartialEntry.BYTES);
    if (partialEntryBytes.length == 0) {
      return Optional.empty();
    } else if (partialEntryBytes.length != PartialEntry.BYTES) {
      throw new StorageLoadException(String.format(
          "SegmentIndex PartialEntry bytes read too short. Expected [%d], actual [%d]",
          PartialEntry.BYTES, partialEntryBytes.length));
    }
    PartialEntry partialEntry = PartialEntry.fromBytes(partialEntryBytes);

    byte[] keyBytes = is.readNBytes(partialEntry.keyLength().value());
    if (keyBytes.length != partialEntry.keyLength().value()) {
      throw new StorageLoadException(String.format(
          "SegmentIndex key bytes read too short. Expected [%d], actual [%d]",
          partialEntry.keyLength().value(), keyBytes.length));
    }
    String key = new String(keyBytes);

    return Optional.of(new SegmentIndexEntry(key, partialEntry.offset()));
  }
}
