package dev.sbutler.bitflask.storage.lsm.memtable;

import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

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
   * Creates a new {@link WriteAheadLog} truncating any pre-existing WriteAheadLog file.
   */
  public static WriteAheadLog create(Path path) throws IOException {
    return new WriteAheadLog(
        Files.newOutputStream(
            path,
            new StandardOpenOption[]{
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE,
                StandardOpenOption.WRITE, StandardOpenOption.DSYNC}));
  }

  /**
   * Creates a {@link WriteAheadLog} from a pre-existing file with new writes appending to it.
   *
   * <p>A file will be created if one does not already exist.
   */
  public static WriteAheadLog createFromPreExisting(Path path) throws IOException {
    return new WriteAheadLog(
        Files.newOutputStream(
            path,
            new StandardOpenOption[]{
                StandardOpenOption.APPEND, StandardOpenOption.CREATE,
                StandardOpenOption.WRITE, StandardOpenOption.DSYNC}));
  }

  public void append(Entry entry) throws IOException {
    logOutputStream.write(entry.getBytes());
    logOutputStream.flush();
  }

  public void close() throws IOException {
    logOutputStream.close();
  }
}
