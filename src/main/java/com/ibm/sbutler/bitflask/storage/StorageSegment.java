package com.ibm.sbutler.bitflask.storage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;

/**
 * Represents a single self contained file for storing data
 */
class StorageSegment {

  public static final Long NEW_SEGMENT_THRESHOLD = 1048576L; // 1 MiB
  private static final String READ_ERR_EOF = "Error retrieving data, end of file";
  private static final String READ_ERR_NO_LENGTH = "Error retrieving data, length 0";
  private static final String READ_ERR_RESULT_LENGTH_LESS = "Error retrieving data, read length (%d) less than provided space (%d)";
  private static final String SEGMENT_ACCESS_LEVEL = "rwd";
  private static final String DEFAULT_SEGMENT_FILE_PATH = "./store/segment%d.txt";
  private static int newSegmentFileIndex = 0;

  private final RandomAccessFile segmentFile;
  private final Map<String, StorageEntry> storageEntryMap = new HashMap<>();
  @Getter
  private final int segmentIndex = newSegmentFileIndex;
  private long currentFileWriteOffset = 0L;

  public StorageSegment() throws FileNotFoundException {
    // todo: handle pre-existing files
    String newSegmentFilePath = String.format(DEFAULT_SEGMENT_FILE_PATH, newSegmentFileIndex);
    segmentFile = new RandomAccessFile(newSegmentFilePath, SEGMENT_ACCESS_LEVEL);
    newSegmentFileIndex++;
  }

  /**
   * Checks if the segment exceeds the new segment threshold
   *
   * @return whether it exceeds the threshold, or not
   */
  public boolean exceedsStorageThreshold() {
    return currentFileWriteOffset > NEW_SEGMENT_THRESHOLD;
  }

  /**
   * Checks if the segment contains the provided key
   *
   * @param key the key to be searched for
   * @return whether it contains the key, or not
   */
  public boolean hasKey(String key) {
    return storageEntryMap.containsKey(key);
  }

  /**
   * Writes the provided key and value to the segment file
   *
   * @param key   the key to be written and saved for retrieving data
   * @param value the associated data value to be written
   * @throws IOException when seeking to the entry's offset or writing fails
   */
  public void write(String key, String value) throws IOException {
    String combinedPair = key + value;

    segmentFile.seek(currentFileWriteOffset);
    segmentFile.write(combinedPair.getBytes(StandardCharsets.UTF_8));
    StorageEntry storageEntry = new StorageEntry(currentFileWriteOffset, key.length(),
        value.length());
    storageEntryMap.put(key, storageEntry);
    currentFileWriteOffset += combinedPair.length();
  }

  /**
   * Reads the provided key's value from the segment file
   *
   * @param key the key to find the data in the segment file
   * @return the value for the key from the segment file, if it exists
   * @throws IOException when seeking to the entry's offset or reading fails
   */
  public Optional<String> read(String key) throws IOException {
    if (!hasKey(key)) {
      return Optional.empty();
    }

    StorageEntry readStorageEntry = storageEntryMap.get(key);
    int entryTotalLength = readStorageEntry.getKeyLength() + readStorageEntry.getValueLength();
    byte[] readBytes = new byte[entryTotalLength];

    segmentFile.seek(readStorageEntry.getSegmentOffset());
    int result = segmentFile.read(readBytes);

    if (result < 0) {
      System.out.printf((READ_ERR_EOF) + "%n");
    } else if (result == 0) {
      System.out.printf((READ_ERR_NO_LENGTH) + "%n");
    } else if (result != readBytes.length) {
      System.out.printf((READ_ERR_RESULT_LENGTH_LESS) + "%n", result, readBytes.length);
    }

    // todo: handle bad reads, return Optional?
    // todo: improve logic
    byte[] valueBytes = new byte[readStorageEntry.getValueLength()];
    for (int i = readStorageEntry.getKeyLength(), j = 0; i < entryTotalLength; i++, j++) {
      valueBytes[j] = readBytes[i];
    }
    return Optional.of(new String(valueBytes));
  }
}
