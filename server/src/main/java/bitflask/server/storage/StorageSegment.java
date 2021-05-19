package bitflask.server.storage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a single self contained file for storing data
 */
class StorageSegment {

  public static final Long NEW_SEGMENT_THRESHOLD = 1048576L; // 1 MiB

  private static final String READ_ERR_BYTES_LESS_THAN_ENTRY =
      "Error retrieving the data, The number of bytes read (%d) was less than the entry's total length (%d)";

  private static final String DEFAULT_SEGMENT_FILE_PATH = "store/segment%d.txt";
  private static final StandardOpenOption[] fileOptions = {StandardOpenOption.CREATE, StandardOpenOption.READ,
      StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING};
  private static final Set<StandardOpenOption> fileChannelOptions = new HashSet<>(
      Arrays.asList(fileOptions));

  private final AsynchronousFileChannel segmentFileChannel;
  private final ConcurrentMap<String, StorageEntry> storageEntryMap = new ConcurrentHashMap<>();
  private final AtomicLong currentFileWriteOffset = new AtomicLong(0);

  /**
   * Creates a new storage segment with a corresponding file to store and retrieve submitted
   * entries
   *
   * @param threadPoolExecutor the thread pool executor service for associating the underlying
   *                           asynchronous file
   * @param segmentIndex       the index used for generating the corresponding segment file
   * @throws IOException when opening the asynchronous file fails
   */
  public StorageSegment(ThreadPoolExecutor threadPoolExecutor, int segmentIndex)
      throws IOException {
    Path newSegmentFilePath = Paths
        .get(String.format(DEFAULT_SEGMENT_FILE_PATH, segmentIndex));
    segmentFileChannel = AsynchronousFileChannel
        .open(newSegmentFilePath, fileChannelOptions, threadPoolExecutor);
  }

  /**
   * Checks if the segment exceeds the new segment threshold
   *
   * @return whether it exceeds the threshold, or not
   */
  public boolean exceedsStorageThreshold() {
    return currentFileWriteOffset.get() > NEW_SEGMENT_THRESHOLD;
  }

  /**
   * Checks if the segment contains the provided key
   *
   * @param key the key to be searched for
   * @return whether it contains the key, or not
   */
  public boolean containsKey(String key) {
    return storageEntryMap.containsKey(key);
  }

  /**
   * Writes the provided key and value to the segment file
   *
   * @param key   the key to be written and saved for retrieving data
   * @param value the associated data value to be written
   */
  public void write(String key, String value) {
    String combinedPair = key + value;
    byte[] combinedPairAry = combinedPair.getBytes(StandardCharsets.UTF_8);
    long offset = currentFileWriteOffset.getAndAdd(combinedPairAry.length);

    Future<Integer> writeFuture = segmentFileChannel
        .write(ByteBuffer.wrap(combinedPairAry), offset);

    StorageEntry storageEntry = new StorageEntry(offset, key.length(),
        value.length());
    try {
      writeFuture.get();
      // Handle newer value being written and added in another thread for same key
      storageEntryMap.merge(key, storageEntry, (retrievedStorageEntry, writtenStorageEntry) ->
          retrievedStorageEntry.getSegmentOffset() < writtenStorageEntry.getSegmentOffset()
              ? writtenStorageEntry
              : retrievedStorageEntry
      );
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }
  }

  /**
   * Reads the provided key's value from the segment file
   *
   * @param key the key to find the data in the segment file
   * @return the value for the key from the segment file, if it exists
   */
  public Optional<String> read(String key) {
    if (!containsKey(key)) {
      return Optional.empty();
    }

    StorageEntry storageEntry = storageEntryMap.get(key);
    ByteBuffer readBytesBuffer = ByteBuffer.allocate(storageEntry.getTotalLength());

    Future<Integer> readFuture = segmentFileChannel
        .read(readBytesBuffer, storageEntry.getSegmentOffset());

    try {
      Integer readBytesLength = readFuture.get();
      if (readBytesLength < storageEntry.getTotalLength()) {
        System.out
            .printf(READ_ERR_BYTES_LESS_THAN_ENTRY, readBytesLength, storageEntry.getTotalLength());
        return Optional.empty();
      }

      String entry = new String(readBytesBuffer.array()).trim();
      readBytesBuffer.clear();

      String value = entry.substring(storageEntry.getKeyLength());
      return Optional.of(value);
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }

    return Optional.empty();
  }
}
