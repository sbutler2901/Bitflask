package dev.sbutler.bitflask.storage.lsm.segment;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.primitives.Longs;
import dev.sbutler.bitflask.common.primitives.UnsignedShort;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.exceptions.StorageLoadException;
import java.io.BufferedInputStream;
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
    try (BufferedInputStream is = new BufferedInputStream(Files.newInputStream(path))) {
      SegmentIndexMetadata metadata =
          SegmentIndexMetadata.fromBytes(is.readNBytes(SegmentIndexMetadata.BYTES));

      ImmutableSortedMap.Builder<String, Long> indexKeyOffsetMap = ImmutableSortedMap.naturalOrder();

      while (true) {
        byte[] keyLengthBytes = is.readNBytes(UnsignedShort.BYTES);
        if (keyLengthBytes.length == 0) {
          break;
        } else if (keyLengthBytes.length != UnsignedShort.BYTES) {
          throw new StorageLoadException(String.format(
              "SegmentIndex keyLength bytes read too short. Expected [%d], actual [%d]",
              UnsignedShort.BYTES, keyLengthBytes.length));
        }
        UnsignedShort keyLength = UnsignedShort.fromBytes(keyLengthBytes);

        byte[] offsetBytes = is.readNBytes(Long.BYTES);
        if (offsetBytes.length != Long.BYTES) {
          throw new StorageLoadException(String.format(
              "SegmentIndex offset bytes read too short. Expected [%d], actual [%d]",
              Long.BYTES, offsetBytes.length));
        }
        long offset = Longs.fromByteArray(offsetBytes);

        byte[] keyBytes = is.readNBytes(keyLength.value());
        if (keyBytes.length != keyLength.value()) {
          throw new StorageLoadException(String.format(
              "SegmentIndex key bytes read too short. Expected [%d], actual [%d]",
              keyLength.value(), keyBytes.length));
        }
        String key = new String(keyBytes);

        indexKeyOffsetMap.put(key, offset);
      }

      return new SegmentIndexDense(metadata, indexKeyOffsetMap.build());
    }
  }
}
