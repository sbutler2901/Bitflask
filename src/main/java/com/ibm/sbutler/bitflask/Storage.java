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

  private void validateArgs(String exceptionPrefix, byte[] bytes, long offset) throws IllegalArgumentException {
    if (bytes == null) {
      throw new IllegalArgumentException(exceptionPrefix + (ARG_PROVIDED_BUFFER_NULL) + "%n");
    } else if (bytes.length == 0) {
      throw new IllegalArgumentException(exceptionPrefix + (ARG_PROVIDED_BUFFER_EMPTY) + "%n");
    } else if (offset < 0) {
      throw new IllegalArgumentException(exceptionPrefix + String.format((ARG_PROVIDED_OFFSET_LESS_THAN_ZERO) + "%n", offset));
    }
  }

  public void write(byte[] bytes, long offset) throws IOException, IllegalArgumentException {
    validateArgs(WRITE_ERR_PREFIX, bytes, offset);

    randomAccessFile.seek(offset);
    randomAccessFile.write(bytes);
  }

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
