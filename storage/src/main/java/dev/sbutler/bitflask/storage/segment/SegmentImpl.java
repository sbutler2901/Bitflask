package dev.sbutler.bitflask.storage.segment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a single self-contained file for storing data
 */
public class SegmentImpl {

  public static final Long NEW_SEGMENT_THRESHOLD = 1048576L; // 1 MiB

  private final SegmentFile segmentFile;
  private final ConcurrentMap<String, Entry> keyStorageEntryMap = new ConcurrentHashMap<>();
  private final AtomicLong currentFileWriteOffset = new AtomicLong(0);

  public SegmentImpl(SegmentFile segmentFile) {
    this.segmentFile = segmentFile;
  }

  /**
   * Writes the provided key and value to the segment file
   *
   * @param key   the key to be written and saved for retrieving data
   * @param value the associated data value to be written
   */
  public void write(String key, String value) {
    byte[] encodedKeyAndValue = encodeKeyAndValue(key, value);
    long writeOffset = currentFileWriteOffset.getAndAdd(encodedKeyAndValue.length);

    try {
      segmentFile.write(encodedKeyAndValue, writeOffset);
      createAndAddNewStorageEntry(key, value, writeOffset);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private byte[] encodeKeyAndValue(String key, String value) {
    String keyAndValueCombined = key + value;
    return keyAndValueCombined.getBytes(StandardCharsets.UTF_8);
  }

  private void createAndAddNewStorageEntry(String key, String value, long offset) {
    Entry storageEntry = new Entry(offset, key.length(), value.length());
    // Handle newer value being written and added in another thread for same key
    keyStorageEntryMap.merge(key, storageEntry, (retrievedStorageEntry, writtenStorageEntry) ->
        retrievedStorageEntry.segmentFileOffset < writtenStorageEntry.segmentFileOffset
            ? writtenStorageEntry
            : retrievedStorageEntry
    );
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

    Entry storageEntry = keyStorageEntryMap.get(key);
    try {
      byte[] readBytes = segmentFile.read(storageEntry.getTotalLength(),
          storageEntry.segmentFileOffset);
      String value = decodeValue(readBytes, storageEntry.keyLength);
      return Optional.of(value);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return Optional.empty();
  }

  private String decodeValue(byte[] readBytes, int keyLength) {
    String entry = new String(readBytes).trim();
    return entry.substring(keyLength);
  }

  /**
   * Checks if the segment contains the provided key
   *
   * @param key the key to be searched for
   * @return whether it contains the key, or not
   */
  public boolean containsKey(String key) {
    return keyStorageEntryMap.containsKey(key);
  }

  /**
   * Checks if the segment exceeds the new segment threshold
   *
   * @return whether it exceeds the threshold, or not
   */
  public boolean exceedsStorageThreshold() {
    return currentFileWriteOffset.get() > NEW_SEGMENT_THRESHOLD;
  }

  record Entry(long segmentFileOffset, int keyLength, int valueLength) {

    private static final String INVALID_ARGS = "Invalid entry values: %d, %d, %d";

    Entry {
      if (segmentFileOffset < 0 || keyLength <= 0 || valueLength <= 0) {
        throw new IllegalArgumentException(
            String.format(INVALID_ARGS, segmentFileOffset, keyLength, valueLength));
      }
    }

    int getTotalLength() {
      return keyLength + valueLength;
    }

    @Override
    public String toString() {
      return "Entry{" +
          "segmentOffset=" + segmentFileOffset +
          ", keyLength=" + keyLength +
          ", valueLength=" + valueLength +
          '}';
    }
  }
}
