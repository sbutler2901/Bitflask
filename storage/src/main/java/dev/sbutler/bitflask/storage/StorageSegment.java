package dev.sbutler.bitflask.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a single self-contained file for storing data
 */
class StorageSegment {

  public static final Long NEW_SEGMENT_THRESHOLD = 1048576L; // 1 MiB

  private final StorageSegmentFile storageSegmentFile;
  private final ConcurrentMap<String, StorageEntry> storageEntryMap = new ConcurrentHashMap<>();
  private final AtomicLong currentFileWriteOffset = new AtomicLong(0);

  public StorageSegment(StorageSegmentFile storageSegmentFile) {
    this.storageSegmentFile = storageSegmentFile;
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

    try {
      storageSegmentFile.write(combinedPairAry, offset);

      StorageEntry storageEntry = new StorageEntry(offset, key.length(),
          value.length());
      // Handle newer value being written and added in another thread for same key
      storageEntryMap.merge(key, storageEntry, (retrievedStorageEntry, writtenStorageEntry) ->
          retrievedStorageEntry.getSegmentOffset() < writtenStorageEntry.getSegmentOffset()
              ? writtenStorageEntry
              : retrievedStorageEntry
      );
    } catch (IOException e) {
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

    try {
      byte[] readBytes = storageSegmentFile.read(storageEntry.getTotalLength(),
          storageEntry.getSegmentOffset());
      String entry = new String(readBytes).trim();
      String value = entry.substring(storageEntry.getKeyLength());
      return Optional.of(value);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return Optional.empty();
  }
}
