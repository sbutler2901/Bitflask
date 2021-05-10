package com.ibm.sbutler.bitflask.storage;

import lombok.Getter;

/**
 * Contains metadata regrading persisted data
 */
final class StorageEntry {

  @Getter
  private final long segmentOffset;
  @Getter
  private final int keyLength, valueLength;

  private void checkArgs(long segmentOffset, int keyLength, int valueLength) {
    if (segmentOffset < 0 || keyLength <= 0 || valueLength <= 0) {
      throw new IllegalArgumentException();
    }
  }

  public StorageEntry(long segmentOffset, int keyLength, int valueLength) {
    checkArgs(segmentOffset, keyLength, valueLength);
    this.segmentOffset = segmentOffset;
    this.keyLength = keyLength;
    this.valueLength = valueLength;
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
