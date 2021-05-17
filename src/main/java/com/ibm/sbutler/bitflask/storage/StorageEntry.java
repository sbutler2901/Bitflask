package com.ibm.sbutler.bitflask.storage;

import lombok.Getter;

/**
 * Contains metadata regrading persisted data
 */
final class StorageEntry {

  private static final String INVALID_ARGS = "The provided constructor arguments were invalid";

  @Getter
  private final long segmentOffset;
  @Getter
  private final int keyLength, valueLength;

  /**
   * Creates a new storage entry to store metadata related to a stored key-value pair
   *
   * @param segmentOffset the segment file offset of this entry
   * @param keyLength     the length of the key described by this entry
   * @param valueLength   the length of the value described by this entry
   */
  public StorageEntry(long segmentOffset, int keyLength, int valueLength) {
    checkArgs(segmentOffset, keyLength, valueLength);
    this.segmentOffset = segmentOffset;
    this.keyLength = keyLength;
    this.valueLength = valueLength;
  }

  private void checkArgs(long segmentOffset, int keyLength, int valueLength) {
    if (segmentOffset < 0 || keyLength <= 0 || valueLength <= 0) {
      throw new IllegalArgumentException(INVALID_ARGS);
    }
  }

  /**
   * Used to get the total length of the storage entry, length of the key and value
   *
   * @return the total length
   */
  public int getTotalLength() {
    return keyLength + valueLength;
  }

  @Override
  public String toString() {
    return "StorageEntry{" +
        "segmentOffset=" + segmentOffset +
        ", keyLength=" + keyLength +
        ", valueLength=" + valueLength +
        '}';
  }
}
