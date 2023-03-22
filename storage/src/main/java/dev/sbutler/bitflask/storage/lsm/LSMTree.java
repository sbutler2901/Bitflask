package dev.sbutler.bitflask.storage.lsm;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import dev.sbutler.bitflask.storage.exceptions.StorageLoadException;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * A Log Structured Merge Tree implementation for reading and writing key:value pairs.
 */
@Singleton
public final class LSMTree {

  private final ListeningScheduledExecutorService scheduledExecutorService;
  private final LSMTreeReader reader;
  private final LSMTreeWriter writer;
  private final LSMTreeLoader loader;
  private final LSMTreeCompactor compactor;

  private final AtomicBoolean isLoaded = new AtomicBoolean(false);

  @Inject
  LSMTree(
      @LSMTreeListeningScheduledExecutorService
      ListeningScheduledExecutorService scheduledExecutorService,
      LSMTreeReader reader,
      LSMTreeWriter writer,
      LSMTreeLoader loader,
      LSMTreeCompactor compactor) {
    this.scheduledExecutorService = scheduledExecutorService;
    this.reader = reader;
    this.writer = writer;
    this.loader = loader;
    this.compactor = compactor;
  }

  /**
   * Reads the value of the provided key and returns it, if present.
   */
  public Optional<String> read(String key) {
    return reader.read(key).filter(Predicate.not(Entry::isDeleted)).map(Entry::value);
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
    scheduledExecutorService.scheduleWithFixedDelay(
        compactor, Duration.ofMinutes(0), Duration.ofMinutes(1));
  }

}
