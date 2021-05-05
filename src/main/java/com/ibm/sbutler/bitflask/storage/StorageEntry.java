package com.ibm.sbutler.bitflask.storage;

import lombok.Getter;

/**
 * Contains metadata regrading persisted data
 */
class StorageEntry {

  @Getter
  private final long segmentOffset;
  @Getter
  private final int keyLength, valueLength;

  public StorageEntry(long segmentOffset, int keyLength, int valueLength)
      throws IllegalArgumentException {
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
