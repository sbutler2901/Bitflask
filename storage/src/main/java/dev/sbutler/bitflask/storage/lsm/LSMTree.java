package dev.sbutler.bitflask.storage.lsm;

import dev.sbutler.bitflask.storage.exceptions.StorageLoadException;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * A Log Structured Merge Tree implementation for reading and writing key:value pairs.
 */
@Singleton
public final class LSMTree {

  private final LSMTreeReader reader;
  private final LSMTreeWriter writer;
  private final LSMTreeLoader loader;

  private final AtomicBoolean isLoaded = new AtomicBoolean(false);

  @Inject
  LSMTree(LSMTreeReader reader, LSMTreeWriter writer, LSMTreeLoader loader) {
    this.reader = reader;
    this.writer = writer;
    this.loader = loader;
  }

  /**
   * Reads the value of the provided key and returns it, if present.
   */
  public Optional<String> read(String key) {
    return reader.read(key).map(Entry::value);
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

  /**
   * Initiates loading of all LSMTree resources.
   */
  public void load() {
    if (isLoaded.getAndSet(true)) {
      throw new StorageLoadException("LSMTree should only be loaded once at startup.");
    }
    loader.load();
  }

}
