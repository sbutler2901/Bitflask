package dev.sbutler.bitflask.storage.segment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import dev.sbutler.bitflask.storage.segment.Encoder.Header;
import dev.sbutler.bitflask.storage.segment.Segment.Entry;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SegmentTest {

  Segment segment;
  @Mock
  SegmentFile segmentFile;
  @Mock
  ConcurrentMap<String, Entry> keyedEntryMap;
  AtomicLong currentFileWriteOffset = new AtomicLong();
  Long segmentSizeLimit = 100L;

  @BeforeEach
  void beforeEach() {
    segment = new Segment(segmentFile, keyedEntryMap, currentFileWriteOffset,
        segmentSizeLimit);
  }

  @Test
  void read_keyValueEntry() throws Exception {
    // Arrange
    String key = "key", value = "value";
    Entry entry = new Entry(Header.KEY_VALUE, 0L);
    doReturn(true).when(segmentFile).isOpen();
    doReturn(true).when(keyedEntryMap).containsKey(key);
    doReturn(entry).when(keyedEntryMap).get(key);
    when(segmentFile.readByte(anyLong()))
        .thenReturn(Header.KEY_VALUE.getByteMap())
        .thenReturn((byte) 5);
    doReturn(value).when(segmentFile).readAsString(anyInt(), anyLong());
    // Act
    Optional<String> result = segment.read(key);
    // Assert
    assertTrue(result.isPresent());
    assertEquals(value, result.get());
    verify(segmentFile, times(1)).readAsString(anyInt(), anyLong());
  }

  @Test
  void read_deletedEntry() throws Exception {
    // Arrange
    String key = "key";
    Entry entry = new Entry(Header.DELETED, 0L);
    doReturn(true).when(segmentFile).isOpen();
    doReturn(true).when(keyedEntryMap).containsKey(key);
    doReturn(entry).when(keyedEntryMap).get(key);
    // Act
    Optional<String> result = segment.read(key);
    // Assert
    assertTrue(result.isEmpty());
  }

  @Test
  void read_exception() throws Exception {
    // Arrange
    String key = "key";
    Entry entry = new Entry(Header.KEY_VALUE, 0L);
    doReturn(true).when(keyedEntryMap).containsKey(key);
    doReturn(true).when(segmentFile).isOpen();
    doReturn(entry).when(keyedEntryMap).get(key);
    when(segmentFile.readByte(anyLong()))
        .thenReturn(Header.KEY_VALUE.getByteMap())
        .thenReturn((byte) 5);
    doThrow(IOException.class).when(segmentFile).readAsString(anyInt(), anyLong());
    // Act
    assertThrows(IOException.class, () -> segment.read(key));
    // Assert
    verify(segmentFile, times(1)).readAsString(anyInt(), anyLong());
  }

  @Test
  void read_keyNotFound() throws Exception {
    // Arrange
    String key = "key";
    doReturn(false).when(keyedEntryMap).containsKey(key);
    // Act
    Optional<String> readResults = segment.read(key);
    // Assert
    assertTrue(readResults.isEmpty());
    verify(segmentFile, times(0)).read(anyInt(), anyLong());
  }

  @Test
  void read_afterClose() {
    // Arrange
    String key = "key";
    doReturn(true).when(keyedEntryMap).containsKey(key);
    doReturn(false).when(segmentFile).isOpen();
    // Act
    RuntimeException e =
        assertThrows(RuntimeException.class, () -> segment.read(key));
    // Assert
    assertTrue(e.getMessage().contains("closed"));
  }

  @Test
  void write() throws Exception {
    // Arrange
    String key = "key", value = "value";
    byte[] encoded = Encoder.encode(Header.KEY_VALUE, key, value);
    doReturn(true).when(segmentFile).isOpen();
    // Act
    segment.write(key, value);
    // Assert
    verify(segmentFile, times(1)).write(aryEq(encoded), anyLong());
    verify(keyedEntryMap, times(1)).put(eq(key), any(Entry.class));
  }

  @Test
  void write_sizeLimitExceededConsumer() throws Exception {
    // Arrange
    segment = new Segment(segmentFile, keyedEntryMap, currentFileWriteOffset, 1);
    doReturn(true).when(segmentFile).isOpen();
    AtomicBoolean wasCalled = new AtomicBoolean(false);
    Consumer<Segment> limitConsumer = (ignored) -> wasCalled.set(true);
    segment.registerSizeLimitExceededConsumer(limitConsumer);
    // Act
    segment.write("key", "value");
    // Assert
    assertTrue(wasCalled.get());
  }

  @Test
  void write_Exception() throws Exception {
    // Arrange
    String key = "key", value = "value";
    doReturn(true).when(segmentFile).isOpen();
    doThrow(IOException.class).when(segmentFile).write(any(), anyLong());
    // Act / Assert
    assertThrows(IOException.class, () -> segment.write(key, value));
    verify(segmentFile, times(1)).write(any(), anyLong());
  }

  @Test
  void write_afterClose() {
    // Arrange
    doReturn(false).when(segmentFile).isOpen();
    // Act
    RuntimeException e =
        assertThrows(RuntimeException.class, () -> segment.write("key", "value"));
    // Assert
    assertTrue(e.getMessage().contains("closed"));
  }

  @Test
  void delete() throws Exception {
    // Act
    String key = "key";
    segment.delete(key);
    // Assert
    verify(keyedEntryMap, times(1)).remove(key);
  }

  @Test
  void containsKey() throws Exception {
    // Arrange
    String key = "key", value = "value";
    assertFalse(segment.containsKey(key));
    doReturn(true).when(segmentFile).isOpen();
    doReturn(true).when(keyedEntryMap).containsKey(key);
    // Act
    segment.write(key, value);
    // Assert
    assertTrue(segment.containsKey(key));
  }

  @Test
  void exceedsStorageThreshold() throws Exception {
    // Arrange
    segment = new Segment(segmentFile, keyedEntryMap, currentFileWriteOffset, 1);
    doReturn(true).when(segmentFile).isOpen();
    segment.write("key", "value");
    // Act / Assert
    assertTrue(segment.exceedsStorageThreshold());
  }

  @Test
  void getSegmentKeys() throws Exception {
    // Arrange
    String key = "key", value = "value";
    doReturn(true).when(segmentFile).isOpen();
    doReturn(ImmutableSet.of(key)).when(keyedEntryMap).keySet();
    // Act
    segment.write(key, value);
    // Assert
    assertTrue(segment.getSegmentKeys().contains(key));
  }

  @Test
  void getSegmentFileKey() {
    // Arrange
    int fileKey = 0;
    doReturn(fileKey).when(segmentFile).getSegmentFileKey();
    // Act
    int segmentFileKey = segment.getSegmentFileKey();
    // Assert
    assertEquals(fileKey, segmentFileKey);
  }

  @Test
  void close() {
    // Act
    segment.close();
    // Assert
    verify(segmentFile, times(1)).close();
    assertFalse(segment.isOpen());
  }

  @Test
  void deleteSegment() throws Exception {
    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      // Act
      segment.close();
      segment.deleteSegment();
      // Assert
      filesMockedStatic.verify(() -> {
        try {
          Files.delete(any());
        } catch (IOException e) {
          fail();
        }
      }, times(1));
    }
  }

  @Test
  void delete_withoutClosing() {
    doReturn(true).when(segmentFile).isOpen();
    assertThrows(RuntimeException.class, () -> segment.deleteSegment());
  }

  @Test
  void compactedCheck() {
    assertFalse(segment.hasBeenCompacted());
    segment.markCompacted();
    assertTrue(segment.hasBeenCompacted());
  }

  @Test
  void toStringTest() {
    assertEquals("segment-0", segment.toString());
  }

}
