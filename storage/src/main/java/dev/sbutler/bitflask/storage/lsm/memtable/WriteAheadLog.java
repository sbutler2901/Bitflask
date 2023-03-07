package dev.sbutler.bitflask.storage.lsm.memtable;

import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import javax.inject.Inject;

/**
 * A write-ahead-log for entries stored in the {@link Memtable}.
 *
 * <p>All writes to a Memtable should be proceeded by writing to a WriteAheadLog. This enables
 * recovering Memtable data that has not been flushed to a
 * {@link dev.sbutler.bitflask.storage.lsm.segment.Segment} in the case of a crash.
 *
 * <p>It is expected that there is only a single WriteAheadLog file at a time which corresponds to
 * the current in memory Memtable.
 */
final class WriteAheadLog implements AutoCloseable {

  /**
   * The filename of WriteHeadLog files.
   */
  public static final String FILE_NAME = "memtable";

  /**
   * The file extension of WriteAheadLog files.
   */
  public static final String FILE_EXTENSION = ".wlog";

  private final OutputStream logOutputStream;


  private WriteAheadLog(OutputStream logOutputStream) {
    this.logOutputStream = logOutputStream;
  }

  /**
   * A factory for creating {@link WriteAheadLog instances}.
   */
  public static class Factory {

    private final StorageConfigurations configurations;

    @Inject
    Factory(StorageConfigurations configurations) {
      this.configurations = configurations;
    }

    /**
     * Creates a new {@link WriteAheadLog} truncating any pre-existing WriteAheadLog file.
     */
    public WriteAheadLog create() throws IOException {
      return new WriteAheadLog(
          Files.newOutputStream(
              getPath(),
              new StandardOpenOption[]{
                  StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE,
                  StandardOpenOption.WRITE, StandardOpenOption.DSYNC}));
    }

    /**
     * Creates a {@link WriteAheadLog} from a pre-existing file with new writes appending to it.
     *
     * <p>A file will be created if one does not already exist.
     */
    public WriteAheadLog createFromPreExisting() throws IOException {
      return new WriteAheadLog(
          Files.newOutputStream(
              getPath(),
              new StandardOpenOption[]{
                  StandardOpenOption.APPEND, StandardOpenOption.CREATE,
                  StandardOpenOption.WRITE, StandardOpenOption.DSYNC}));
    }

    private Path getPath() {
      return Path.of(
          configurations.getStorageStoreDirectoryPath().toString(),
          FILE_NAME + FILE_EXTENSION);
    }
  }

  public void append(Entry entry) throws IOException {
    logOutputStream.write(entry.getBytes());
    logOutputStream.flush();
  }

  public void close() throws IOException {
    logOutputStream.close();
  }
}
