package dev.sbutler.bitflask.storage.lsm;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import dev.sbutler.bitflask.storage.exceptions.StorageException;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import java.time.Instant;
import java.util.Optional;
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

  private volatile boolean isClosed = false;

  @Inject
  LSMTree(
      @LSMTreeListeningScheduledExecutorService
      ListeningScheduledExecutorService scheduledExecutorService,
      LSMTreeReader reader,
      LSMTreeWriter writer) {
    this.scheduledExecutorService = scheduledExecutorService;
    this.reader = reader;
    this.writer = writer;
  }

  /**
   * Reads the value of the provided key and returns it, if present.
   */
  public Optional<String> read(String key) {
    checkOpenOrThrow();
    return reader.read(key).filter(Predicate.not(Entry::isDeleted)).map(Entry::value);
  }

  /**
   * Writes the provided key:value pair.
   */
  public void write(String key, String value) {
    checkOpenOrThrow();
    Entry entry = new Entry(Instant.now().getEpochSecond(), key, value);
    writer.write(entry);
  }

  /**
   * Deletes the key and any associated entry.
   */
  public void delete(String key) {
    checkOpenOrThrow();
    write(key, "");
  }

  private void checkOpenOrThrow() {
    if (isClosed) {
      throw new StorageException("The LSMTree is closed");
    }
  }

  /**
   * Closes the LSMTree and all dependencies.
   *
   * <p>Blocks until fully closed.
   */
  public void close() {
    // TODO: implement fully.
    isClosed = true;
    scheduledExecutorService.close();
  }
}
