package dev.sbutler.bitflask.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

// todo use mockito annotations
public class StorageSegmentTest {

  @Test
  void write() throws IOException {
    String key = "key", value0 = "value0", value1 = "value1";

    StorageSegmentFile storageSegmentFile = mock(StorageSegmentFile.class);
    StorageSegment storageSegment = new StorageSegment(storageSegmentFile);

    storageSegment.write(key, value0);
    verify(storageSegmentFile, times(1)).write(any(), anyLong());

    storageSegment.write(key, value1);
    verify(storageSegmentFile, times(2)).write(any(), anyLong());
  }

  @Test
  void write_Exception() throws IOException {
    String key = "key", value = "value";

    StorageSegmentFile storageSegmentFile = mock(StorageSegmentFile.class);
    StorageSegment storageSegment = new StorageSegment(storageSegmentFile);

    doThrow(IOException.class).when(storageSegmentFile).write(any(), anyLong());
    storageSegment.write(key, value);
    verify(storageSegmentFile, times(1)).write(any(), anyLong());
  }

  @Test
  void read() throws IOException {
    String key = "key", value = "value", combined = key + value;

    StorageSegmentFile storageSegmentFile = mock(StorageSegmentFile.class);
    StorageSegment storageSegment = new StorageSegment(storageSegmentFile);

    // write before reading
    storageSegment.write(key, value);

    // read
    doReturn(combined.getBytes()).when(storageSegmentFile).read(anyInt(), anyLong());

    Optional<String> result = storageSegment.read(key);

    assertTrue(result.isPresent());
    assertEquals(value, result.get());
    verify(storageSegmentFile, times(1)).read(anyInt(), anyLong());
  }

  @Test
  void read_exception() throws IOException {
    String key = "key", value = "value", combined = key + value;

    StorageSegmentFile storageSegmentFile = mock(StorageSegmentFile.class);
    StorageSegment storageSegment = new StorageSegment(storageSegmentFile);

    // write before reading
    storageSegment.write(key, value);

    // read
    doThrow(IOException.class).when(storageSegmentFile).read(anyInt(), anyLong());

    Optional<String> result = storageSegment.read(key);

    assertTrue(result.isEmpty());
    verify(storageSegmentFile, times(1)).read(anyInt(), anyLong());
  }

  @Test
  void read_keyNotFound() throws IOException {
    String key = "key";

    StorageSegmentFile storageSegmentFile = mock(StorageSegmentFile.class);
    StorageSegment storageSegment = new StorageSegment(storageSegmentFile);

    assertTrue(storageSegment.read(key).isEmpty());
    verify(storageSegmentFile, times(0)).read(anyInt(), anyLong());
  }

  @Test
  void exceedsStorageThreshold() {
    StorageSegmentFile storageSegmentFile = mock(StorageSegmentFile.class);
    StorageSegment storageSegment = new StorageSegment(storageSegmentFile);
    assertFalse(storageSegment.exceedsStorageThreshold());
    // todo: test threshold exceeded
  }
}
