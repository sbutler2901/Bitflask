package dev.sbutler.bitflask.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StorageEntryTest {

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

  @Test
  void getTotalLength() {
    StorageEntry storageEntry = new StorageEntry(0, 5, 10);
    assertEquals(15, storageEntry.getTotalLength());
  }

  @Test
  void getters() {
    StorageEntry storageEntry = new StorageEntry(0, 5, 10);
    assertEquals(0, storageEntry.getSegmentOffset());
    assertEquals(5, storageEntry.getKeyLength());
    assertEquals(10, storageEntry.getValueLength());
  }

  @Test
  void entry_toString() {
    StorageEntry storageEntry = new StorageEntry(0, 5, 10);
    String expected = "StorageEntry{segmentOffset=0, keyLength=5, valueLength=10}";
    assertEquals(expected, storageEntry.toString());
  }
}