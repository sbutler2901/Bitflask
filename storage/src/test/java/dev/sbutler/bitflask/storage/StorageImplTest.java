package dev.sbutler.bitflask.storage;

import java.util.Optional;
import java.util.concurrent.ThreadPoolExecutor;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class StorageImplTest {

  @Test
  void write_belowThreshold() throws IOException {
    try (MockedConstruction<StorageSegment> storageSegmentMockedConstruction = mockConstruction(
        StorageSegment.class)) {
      Storage storage = new StorageImpl(mock(ThreadPoolExecutor.class));
      StorageSegment activeStorageSegment = storageSegmentMockedConstruction.constructed().get(0);
      doReturn(false).when(activeStorageSegment).exceedsStorageThreshold();
      storage.write("key", "value");
      assertEquals(1, storageSegmentMockedConstruction.constructed().size());
    }
  }

  @Test
  void write_aboveThreshold() throws IOException {
    try (MockedConstruction<StorageSegment> storageSegmentMockedConstruction = mockConstruction(
        StorageSegment.class)) {
      Storage storage = new StorageImpl(mock(ThreadPoolExecutor.class));
      StorageSegment activeStorageSegment = storageSegmentMockedConstruction.constructed().get(0);
      doReturn(true).when(activeStorageSegment).exceedsStorageThreshold();
      storage.write("key", "value");
      assertEquals(2, storageSegmentMockedConstruction.constructed().size());
    }
  }

  @Test
  void write_IllegalArgumentException_key() throws IOException {
    try (MockedConstruction<StorageSegment> ignored = mockConstruction(StorageSegment.class)) {
      Storage storage = new StorageImpl(mock(ThreadPoolExecutor.class));
      assertThrows(IllegalArgumentException.class, () -> storage.write(null, "value"));
      assertThrows(IllegalArgumentException.class, () -> storage.write("", "value"));
    }
  }

  @Test
  void write_IllegalArgumentException_value() throws IOException {
    try (MockedConstruction<StorageSegment> ignored = mockConstruction(StorageSegment.class)) {
      Storage storage = new StorageImpl(mock(ThreadPoolExecutor.class));
      assertThrows(IllegalArgumentException.class, () -> storage.write("key", null));
      assertThrows(IllegalArgumentException.class, () -> storage.write("key", ""));
    }
  }

  @Test
  void read_keyFound() throws IOException {
    String key = "key", value = "value";
    try (MockedConstruction<StorageSegment> storageSegmentMockedConstruction = mockConstruction(
        StorageSegment.class)) {
      Storage storage = new StorageImpl(mock(ThreadPoolExecutor.class));
      StorageSegment activeStorageSegment = storageSegmentMockedConstruction.constructed().get(0);
      doReturn(true).when(activeStorageSegment).containsKey(key);
      doReturn(Optional.of(value)).when(activeStorageSegment).read(key);
      Optional<String> result = storage.read(key);
      assertTrue(result.isPresent());
      assertEquals(value, result.get());
    }
  }

  @Test
  void read_keyNotFound() throws IOException {
    String key = "key";
    try (MockedConstruction<StorageSegment> storageSegmentMockedConstruction = mockConstruction(
        StorageSegment.class)) {
      Storage storage = new StorageImpl(mock(ThreadPoolExecutor.class));
      StorageSegment activeStorageSegment = storageSegmentMockedConstruction.constructed().get(0);
      doReturn(false).when(activeStorageSegment).containsKey(key);
      Optional<String> result = storage.read(key);
      assertTrue(result.isEmpty());
    }
  }

  @Test
  void read_IllegalArgumentException() throws IOException {
    try (MockedConstruction<StorageSegment> ignored = mockConstruction(StorageSegment.class)) {
      Storage storage = new StorageImpl(mock(ThreadPoolExecutor.class));
      assertThrows(IllegalArgumentException.class, () -> storage.read(null));
      assertThrows(IllegalArgumentException.class, () -> storage.read(""));
    }
  }
}