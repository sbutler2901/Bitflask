package com.ibm.sbutler.bitflask.Storage;

import com.ibm.sbutler.bitflask.storage.StorageEntry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StorageEntryTest {
  @Test
  void constructor_success() {
    StorageEntry storageEntry = new StorageEntry(0, 0, 10);
    assertEquals(storageEntry.getSegmentIndex(), 0);
    assertEquals(storageEntry.getSegmentOffset(), 0);
    assertEquals(storageEntry.getEntryLength(), 10);
  }

  @Test
  void constructor_invalidArgs() {
    // segmentIndex
    assertThrows(
        IllegalArgumentException.class,
        () -> new StorageEntry(-1, 0, 10)
    );

    // segmentOffset
    assertThrows(
        IllegalArgumentException.class,
        () -> new StorageEntry(0, -1, 10)
    );

    // entryLength
    assertThrows(
        IllegalArgumentException.class,
        () -> new StorageEntry(0, 0, 0)
    );
  }
}