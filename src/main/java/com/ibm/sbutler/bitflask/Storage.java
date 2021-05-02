package com.ibm.sbutler.bitflask;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class Storage {
  private static final String WRITE_ERR_PREFIX = "Error writing data, ";
  private static final String READ_ERR_PREFIX = "Error reading data, ";

  private static final String ARG_PROVIDED_BUFFER_NULL = "provided buffer was null";
  private static final String ARG_PROVIDED_BUFFER_EMPTY = "provided buffer length 0";
  private static final String ARG_PROVIDED_OFFSET_LESS_THAN_ZERO = "provided offset less than 0: (%d)";

  private static final String READ_ERR_EOF = "Error retrieving data, end of file";
  private static final String READ_ERR_NO_LENGTH = "Error retrieving data, length 0";
  private static final String READ_ERR_RESULT_LENGTH_LESS = "Error retrieving data, read length (%d) less than provided space (%d)";

  private static final String filePath = "./store/test.txt";

  private final RandomAccessFile randomAccessFile;

  public Storage() throws FileNotFoundException {
    this(filePath);
  }

  public Storage(String filePath) throws FileNotFoundException {
    this(new RandomAccessFile(filePath, "rwd"));
  }

  public Storage(RandomAccessFile randomAccessFile) {
    this.randomAccessFile = randomAccessFile;
  }

  /**
   * Validates arguments provided when reading or writing data
   * @param exceptionPrefix the prefix to used when creating an exception string
   * @param bytes the buffer to be written or read to
   * @param offset the offset for reading or writing data
   * @throws IllegalArgumentException when an invalid argument is provided
   */
  private void validateArgs(String exceptionPrefix, byte[] bytes, long offset) throws IllegalArgumentException {
    if (bytes == null) {
      throw new IllegalArgumentException(exceptionPrefix + (ARG_PROVIDED_BUFFER_NULL) + "%n");
    } else if (bytes.length == 0) {
      throw new IllegalArgumentException(exceptionPrefix + (ARG_PROVIDED_BUFFER_EMPTY) + "%n");
    } else if (offset < 0) {
      throw new IllegalArgumentException(exceptionPrefix + String.format((ARG_PROVIDED_OFFSET_LESS_THAN_ZERO) + "%n", offset));
    }
  }

  /**
   * Writes the provided data to the current segment file
   * @param bytes the bytes to be written
   * @param offset the file offset to start writing the data
   * @throws IOException when seeking to the provided offset or writing fails
   * @throws IllegalArgumentException when an invalid set of data or offset is provided
   */
  public void write(byte[] bytes, long offset) throws IOException, IllegalArgumentException {
    // TODO: returns object containing offset used for writing data & the data segment file used. Read will need data file number
    validateArgs(WRITE_ERR_PREFIX, bytes, offset);

    randomAccessFile.seek(offset);
    randomAccessFile.write(bytes);
  }

  /**
   * Reads data starting at the provided offset until the provided buffer is full, or an end of file occurs
   * @param bytes the buffer to store read data
   * @param offset the offset to start reading data
   * @throws IOException when seeking or reading fails
   * @throws IllegalArgumentException when an invalid buffer or offset is provided
   */
  public void read(byte[] bytes, long offset) throws IOException, IllegalArgumentException {
    validateArgs(READ_ERR_PREFIX, bytes, offset);

    randomAccessFile.seek(offset);
    int result = randomAccessFile.read(bytes);

    if (result < 0) {
      System.out.printf((READ_ERR_EOF) + "%n");
    } else if (result == 0) {
      System.out.printf((READ_ERR_NO_LENGTH) + "%n");
    } else if (result != bytes.length) {
      System.out.printf((READ_ERR_RESULT_LENGTH_LESS) + "%n", result, bytes.length);
    }
  }
}
