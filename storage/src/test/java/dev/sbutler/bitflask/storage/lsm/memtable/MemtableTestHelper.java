package dev.sbutler.bitflask.storage.lsm.memtable;

import static org.mockito.Mockito.mock;

/**
 * A test helper class for creating {@link Memtable}s.
 */
public final class MemtableTestHelper {

  public static Memtable createMemtableWithMockWriteAheadLog() {
    return Memtable.create(mock(WriteAheadLog.class));
  }

}
