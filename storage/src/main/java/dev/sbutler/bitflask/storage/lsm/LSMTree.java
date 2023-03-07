package dev.sbutler.bitflask.storage.lsm;

import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import java.time.Instant;
import java.util.Optional;
import javax.inject.Inject;

/**
 * A Log Structured Merge Tree implementation for reading and writing key:value pairs.
 */
public final class LSMTree {

  private final LSMTreeReader reader;
  private final LSMTreeWriter writer;

  @Inject
  LSMTree(LSMTreeReader reader, LSMTreeWriter writer) {
    this.reader = reader;
    this.writer = writer;
  }

  /**
   * Reads the value of the provided key and returns it, if present.
   */
  public Optional<String> read(String key) {
    return reader.read(key);
  }

  /**
   * Writes the provided key:value pair.
   */
  public void write(String key, String value) {
    Entry entry = new Entry(Instant.now().getEpochSecond(), key, value);
    writer.write(entry);
  }

  /**
   * Deletes the key and any associated entry.
   */
  public void delete(String key) {
    write(key, "");
  }
}
