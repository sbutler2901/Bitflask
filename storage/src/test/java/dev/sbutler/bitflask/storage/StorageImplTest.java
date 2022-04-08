package dev.sbutler.bitflask.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StorageImplTest {

  @InjectMocks
  StorageImpl storage;
  @Mock
  StorageSegmentManager storageSegmentManager;

  @Test
  void write() throws IOException {
    String key = "key", value = "value";
    StorageSegment storageSegment = mock(StorageSegment.class);
    doReturn(storageSegment).when(storageSegmentManager).getActiveSegment();
    storage.write(key, value);
    verify(storageSegment, times(1)).write(key, value);
  }

  @Test
  void write_IllegalArgumentException_key() {
    assertThrows(IllegalArgumentException.class, () -> storage.write(null, "value"));
    assertThrows(IllegalArgumentException.class, () -> storage.write("", "value"));
  }

  @Test
  void write_IllegalArgumentException_value() {
    assertThrows(IllegalArgumentException.class, () -> storage.write("key", null));
    assertThrows(IllegalArgumentException.class, () -> storage.write("key", ""));
  }

  @Test
  @SuppressWarnings("unchecked")
  void read_keyFound() {
    String key = "key", value = "value";
    Iterator<StorageSegment> segmentIterator = mock(Iterator.class);
    StorageSegment activeStorageSegment = mock(StorageSegment.class);

    doReturn(segmentIterator).when(storageSegmentManager).getStorageSegmentsIterator();
    when(segmentIterator.hasNext()).thenReturn(true).thenReturn(false);
    doReturn(activeStorageSegment).when(segmentIterator).next();
    doReturn(true).when(activeStorageSegment).containsKey(key);
    doReturn(Optional.of(value)).when(activeStorageSegment).read(key);

    Optional<String> result = storage.read(key);
    assertTrue(result.isPresent());
    assertEquals(value, result.get());
  }

  @Test
  @SuppressWarnings("unchecked")
  void read_keyNotFound() {
    String key = "key";
    Iterator<StorageSegment> segmentIterator = mock(Iterator.class);
    StorageSegment activeStorageSegment = mock(StorageSegment.class);

    doReturn(segmentIterator).when(storageSegmentManager).getStorageSegmentsIterator();
    when(segmentIterator.hasNext()).thenReturn(true).thenReturn(false);
    doReturn(activeStorageSegment).when(segmentIterator).next();
    doReturn(false).when(activeStorageSegment).containsKey(key);

    Optional<String> result = storage.read(key);
    assertTrue(result.isEmpty());
  }

  @Test
  void read_IllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> storage.read(null));
    assertThrows(IllegalArgumentException.class, () -> storage.read(""));
  }
}
