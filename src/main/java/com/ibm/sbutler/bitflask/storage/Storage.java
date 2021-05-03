package com.ibm.sbutler.bitflask.storage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages persisting and retrieving data
 */
public class Storage {
  private static final String WRITE_ERR_BUFFER_NULL = "Error writing data, provided buffer was null";
  private static final String WRITE_ERR_BUFFER_EMPTY = "Error writing data, provided buffer length 0";

  private static final String READ_ERR_ENTRY_NULL = "Error retrieving data, the provided storage entry was null";
  private static final String READ_ERR_ENTRY_SEGMENT_INDEX_TOO_LARGE =
      "Error retrieving data, the provided storage entry's segment index is greater than available segments";
  private static final String READ_ERR_ENTRY_OFFSET_TOO_LARGE =
      "Error retrieving data, the provided storage entry's offset was greater than segment's length";
  private static final String READ_ERR_ENTRY_OFFSET_AND_LENGTH_TOO_LARGE =
      "Error retrieving data, the provided storage entry's offset and length was greater than provided segment file's length";
  private static final String READ_ERR_EOF = "Error retrieving data, end of file";
  private static final String READ_ERR_NO_LENGTH = "Error retrieving data, length 0";
  private static final String READ_ERR_RESULT_LENGTH_LESS = "Error retrieving data, read length (%d) less than provided space (%d)";

  private static final String NEW_SEGMENT_FILE_CREATED = "A new segment file was created with index (%d) at (%s)";

  private static final String DEFAULT_SEGMENT_FILE_PATH = "./store/segment%d.txt";
  public static final Long NEW_SEGMENT_THRESHOLD = 10240L; // 10 KiB
  private static final String SEGMENT_ACCESS_LEVEL = "rwd";

  private final List<RandomAccessFile> segmentFilesList = new ArrayList<>();

  public Storage() throws FileNotFoundException {
    createNewSegmentFile();
  }

  public Storage(RandomAccessFile randomAccessFile) {
    segmentFilesList.add(randomAccessFile);
  }

  /**
   * Creates a new segment file for storing data
   *
   * @throws FileNotFoundException when there is an error creating the new segment file
   */
  private void createNewSegmentFile() throws FileNotFoundException {
    int newSegmentFileIndex = segmentFilesList.size();
    String newSegmentFilePath = String.format(DEFAULT_SEGMENT_FILE_PATH, newSegmentFileIndex);
    RandomAccessFile newSegmentFile = new RandomAccessFile(newSegmentFilePath, SEGMENT_ACCESS_LEVEL);

    segmentFilesList.add(newSegmentFile);

    System.out.printf(NEW_SEGMENT_FILE_CREATED, newSegmentFileIndex, newSegmentFilePath);
  }

  /**
   * Gets the file index for the active segment
   *
   * @return the file index
   */
  public int getActiveSegmentFileIndex() {
    return segmentFilesList.size() - 1;
  }

  /**
   * Writes the provided data to the current segment file
   *
   * @param bytes the data to be written
   * @return the entry detailing metadata regarding written data
   * @throws IOException              when seeking to the provided offset or writing fails
   * @throws IllegalArgumentException when an invalid set of data or offset is provided
   */
  public StorageEntry write(byte[] bytes) throws IOException, IllegalArgumentException {
    if (bytes == null) {
      throw new IllegalArgumentException(WRITE_ERR_BUFFER_NULL);
    } else if (bytes.length == 0) {
      throw new IllegalArgumentException(WRITE_ERR_BUFFER_EMPTY);
    }

    int activeSegmentFileIndex = getActiveSegmentFileIndex();
    RandomAccessFile activeSegmentFile = segmentFilesList.get(activeSegmentFileIndex);
    long activeSegmentOffset = activeSegmentFile.length();
    activeSegmentFile.seek(activeSegmentOffset);
    activeSegmentFile.write(bytes);
    StorageEntry storageEntry = new StorageEntry(activeSegmentFileIndex, activeSegmentOffset, bytes.length);
    activeSegmentOffset += bytes.length;

    if (activeSegmentOffset >= NEW_SEGMENT_THRESHOLD) {
      createNewSegmentFile();
    }

    return storageEntry;
  }

  /**
   * Reads data starting at the provided offset until the provided buffer is full, or an end of file occurs
   *
   * @param storageEntry the metadata for the entry to be retrieved
   * @throws IOException              when seeking or reading fails
   * @throws IllegalArgumentException when an invalid buffer or offset is provided
   */
  public byte[] read(StorageEntry storageEntry) throws IOException, IllegalArgumentException {
    if (storageEntry == null) {
      throw new IllegalArgumentException(READ_ERR_ENTRY_NULL);
    } else if (storageEntry.getSegmentIndex() > getActiveSegmentFileIndex()) {
      throw new IllegalArgumentException(READ_ERR_ENTRY_SEGMENT_INDEX_TOO_LARGE);
    }

    RandomAccessFile segmentFileContainingEntry = segmentFilesList.get(storageEntry.getSegmentIndex());

    if (storageEntry.getSegmentOffset() > segmentFileContainingEntry.length()) {
      throw new IllegalArgumentException(READ_ERR_ENTRY_OFFSET_TOO_LARGE);
    } else if ((storageEntry.getSegmentOffset() + storageEntry.getEntryLength()) > segmentFileContainingEntry.length()) {
      throw new IllegalArgumentException(READ_ERR_ENTRY_OFFSET_AND_LENGTH_TOO_LARGE);
    }

    byte[] readBytes = new byte[storageEntry.getEntryLength()];
    segmentFileContainingEntry.seek(storageEntry.getSegmentOffset());
    int result = segmentFileContainingEntry.read(readBytes);

    if (result < 0) {
      System.out.printf((READ_ERR_EOF) + "%n");
    } else if (result == 0) {
      System.out.printf((READ_ERR_NO_LENGTH) + "%n");
    } else if (result != readBytes.length) {
      System.out.printf((READ_ERR_RESULT_LENGTH_LESS) + "%n", result, readBytes.length);
    }

    return readBytes;
  }
}
