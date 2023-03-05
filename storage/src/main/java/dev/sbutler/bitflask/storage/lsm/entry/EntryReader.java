package dev.sbutler.bitflask.storage.lsm.entry;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

/**
 * Enables reading {@link Entry}s from a provided file.
 */
public final class EntryReader {

  private final Path filePath;

  private EntryReader(Path filePath) {
    this.filePath = filePath;
  }

  /**
   * Creates an {@link EntryReader} for retrieving {@link Entry}s from the file located at the
   * provided {@link Path}.
   */
  public static EntryReader create(Path filePath) {
    return new EntryReader(filePath);
  }

  /**
   * Iterates the {@link Entry}s in the associated file until one with the provided key is found, or
   * the end of the segment file is reached.
   *
   * <p>An {@link IOException} will be thrown if there is an issue iterating the entries.
   */
  public Optional<Entry> findEntryFromOffset(String key, long startOffset) throws IOException {
    try (BufferedInputStream is =
        new BufferedInputStream(Files.newInputStream(filePath, StandardOpenOption.READ))) {
      is.skipNBytes(startOffset);

      byte[] metadataBuffer = new byte[EntryMetadata.BYTES];
      while (is.read(metadataBuffer) != -1) {
        EntryMetadata entryMetadata = EntryMetadata.fromBytes(metadataBuffer);
        Optional<Entry> entry = readEntry(is, entryMetadata, key);
        if (entry.isPresent()) {
          return entry;
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Reads the {@link Entry} based on the provided {@link EntryMetadata} and returns it if the
   * {@code key} matches.
   *
   * <p>The provided {@link BufferedInputStream} will be in the correct position for reading the
   * next entry after this method completes.
   */
  private Optional<Entry> readEntry(BufferedInputStream is, EntryMetadata entryMetadata, String key)
      throws IOException {
    byte[] keyBuffer = is.readNBytes(entryMetadata.getKeyLength());
    if (keyBuffer.length != entryMetadata.getKeyLength()) {
      throw new IOException(String.format(
          "Read key length did not match entry. Read [%d], expected [%d].",
          keyBuffer.length, entryMetadata.getKeyLength()));
    }
    String readKey = new String(keyBuffer);

    if (key.equals(readKey)) {
      byte[] valueBuffer = is.readNBytes(entryMetadata.getValueLength());
      if (valueBuffer.length != entryMetadata.getValueLength()) {
        throw new IOException(String.format(
            "Read value length did not match entry. Read [%d], expected [%d].",
            valueBuffer.length, entryMetadata.getValueLength()));
      }
      String value = new String(valueBuffer);

      return Optional.of(new Entry(entryMetadata.creationEpochSeconds(), key, value));
    }

    is.skipNBytes(entryMetadata.getValueLength());
    return Optional.empty();
  }
}
