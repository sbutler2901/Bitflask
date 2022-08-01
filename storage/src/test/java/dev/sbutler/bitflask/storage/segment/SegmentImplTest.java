package dev.sbutler.bitflask.storage.segment;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SegmentImplTest {

  SegmentImpl segment;
  @Mock
  SegmentFile segmentFile;
  @Mock
  ConcurrentMap<String, Long> keyedEntryFileOffsetMap;
  AtomicLong currentFileWriteOffset = new AtomicLong();
  Long segmentSizeLimit = 100L;

  @BeforeEach
  void beforeEach() {
    segment = new SegmentImpl(segmentFile, keyedEntryFileOffsetMap, currentFileWriteOffset,
        segmentSizeLimit);
  }

  @Test
  void write() throws Exception {
    // Arrange
    String key = "key", value = "value";
    byte[] encoded = SegmentImpl.encodeKeyAndValue(key, value);
    doReturn(true).when(segmentFile).isOpen();
    // Act
    segment.write(key, value);
    // Assert
    verify(segmentFile, times(1)).write(aryEq(encoded), anyLong());
  }

  @Test
  @SuppressWarnings("unchecked")
  void write_merge() throws Exception {
    // Arrange
    doReturn(true).when(segmentFile).isOpen();
    ArgumentCaptor<BiFunction<Long, Long, Long>> invokeAllArgumentCaptor =
        ArgumentCaptor.forClass(BiFunction.class);

    // Act
    segment.write("key", "value");
    verify(keyedEntryFileOffsetMap).merge(anyString(), anyLong(),
        invokeAllArgumentCaptor.capture());
    BiFunction<Long, Long, Long> mergeBiFunction = invokeAllArgumentCaptor.getValue();

    // Assert
    assertEquals(5L, mergeBiFunction.apply(0L, 5L));
    assertEquals(5L, mergeBiFunction.apply(5L, 0L));
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
    doReturn(false).when(segmentFile).isOpen();
    assertThrows(RuntimeException.class, () -> segment.write("key", "value"));
  }

  @Test
  void read() throws Exception {
    // Arrange
    String key = "key", value = "value";
    doReturn(true).when(segmentFile).isOpen();
    doReturn(true).when(keyedEntryFileOffsetMap).containsKey(key);
    doReturn(0L).when(keyedEntryFileOffsetMap).get(key);
    doReturn((byte) 5).when(segmentFile).readByte(anyLong());
    doReturn(value).when(segmentFile).readAsString(anyInt(), anyLong());
    // Act
    Optional<String> result = segment.read(key);
    // Assert
    assertTrue(result.isPresent());
    assertEquals(value, result.get());
    verify(segmentFile, times(1)).readAsString(anyInt(), anyLong());
  }

  @Test
  void read_exception() throws Exception {
    // Arrange
    String key = "key";
    doReturn(true).when(segmentFile).isOpen();
    doReturn(true).when(keyedEntryFileOffsetMap).containsKey(key);
    doReturn(0L).when(keyedEntryFileOffsetMap).get(key);
    doReturn((byte) 5).when(segmentFile).readByte(anyLong());
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
    doReturn(true).when(segmentFile).isOpen();
    // Act
    Optional<String> readResults = segment.read(key);
    // Assert
    assertTrue(readResults.isEmpty());
    verify(segmentFile, times(0)).read(anyInt(), anyLong());
  }

  @Test
  void read_afterClose() {
    doReturn(false).when(segmentFile).isOpen();
    assertThrows(RuntimeException.class, () -> segment.read("key"));
  }

  @Test
  void encodedKeyAndValue() {
    // Arrange
    String key = "key", value = "value";
    char keyLengthEncoded = (char) key.length();
    char valueLengthEncoded = (char) value.length();
    byte[] expected = (keyLengthEncoded + key + valueLengthEncoded + value).getBytes();
    // Act
    byte[] encoded = SegmentImpl.encodeKeyAndValue(key, value);
    // Assert
    assertArrayEquals(expected, encoded);
  }

  @Test
  void encodedKeyAndValue_key_invalidArg() {
    assertThrows(NullPointerException.class, () -> SegmentImpl.encodeKeyAndValue(null, "value"));
    assertThrows(IllegalArgumentException.class, () -> SegmentImpl.encodeKeyAndValue("", "value"));
    assertThrows(IllegalArgumentException.class,
        () -> SegmentImpl.encodeKeyAndValue(new String(new byte[257]), "value"));
  }

  @Test
  void encodedKeyAndValue_value_invalidArg() {
    assertThrows(NullPointerException.class, () -> SegmentImpl.encodeKeyAndValue("key", null));
    assertThrows(IllegalArgumentException.class, () -> SegmentImpl.encodeKeyAndValue("key", ""));
    assertThrows(IllegalArgumentException.class,
        () -> SegmentImpl.encodeKeyAndValue("key", new String(new byte[257])));
  }

  @Test
  void containsKey() throws Exception {
    // Arrange
    String key = "key", value = "value";
    assertFalse(segment.containsKey(key));
    doReturn(true).when(segmentFile).isOpen();
    doReturn(true).when(keyedEntryFileOffsetMap).containsKey(key);
    // Act
    segment.write(key, value);
    // Assert
    assertTrue(segment.containsKey(key));
  }

  @Test
  void exceedsStorageThreshold() throws Exception {
    // Arrange
    segment = new SegmentImpl(segmentFile, keyedEntryFileOffsetMap, currentFileWriteOffset, 1);
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
    doReturn(ImmutableSet.of(key)).when(keyedEntryFileOffsetMap).keySet();
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
  void delete() throws Exception {
    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      // Act
      segment.close();
      segment.delete();
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
    assertThrows(RuntimeException.class, () -> segment.delete());
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
