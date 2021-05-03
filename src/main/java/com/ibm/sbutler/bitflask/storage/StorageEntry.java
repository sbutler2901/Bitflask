package com.ibm.sbutler.bitflask.storage;

import lombok.Getter;

/**
 * Contains metadata regrading persisted data
 */
public class StorageEntry {
  private static final String ERR_NEGATIVE_SEGMENT_INDEX = "A storage entry must have a positive segment index";
  private static final String ERR_NEGATIVE_SEGMENT_OFFSET = "A storage entry must have a positive segment offset";
  private static final String ERR_ENTRY_LENGTH_TOO_SHORT = "A storage entry must have a length greater than 0";

  @Getter
  private final int segmentIndex;
  @Getter
  private final long segmentOffset;
  @Getter
  private final int entryLength;

  public StorageEntry(int segmentIndex, long segmentOffset, int entryLength) throws IllegalArgumentException {
    if (segmentIndex < 0) {
      throw new IllegalArgumentException(ERR_NEGATIVE_SEGMENT_INDEX);
    } else if (segmentOffset < 0) {
      throw new IllegalArgumentException(ERR_NEGATIVE_SEGMENT_OFFSET);
    } else if (entryLength <= 0) {
      throw new IllegalArgumentException(ERR_ENTRY_LENGTH_TOO_SHORT);
    }

    this.segmentIndex = segmentIndex;
    this.segmentOffset = segmentOffset;
    this.entryLength = entryLength;
  }

  @Override
  public String toString() {
    return "StorageEntry{" +
        "segmentIndex=" + segmentIndex +
        ", segmentOffset=" + segmentOffset +
        ", entryLength=" + entryLength +
        '}';
  }
}
