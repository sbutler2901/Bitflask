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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SegmentImplTest {

  @InjectMocks
  SegmentImpl segment;
  @Mock
  SegmentFile segmentFile;

  @BeforeEach
  void beforeEach() {
  }

  @Test
  void loadFileEntries() throws IOException {
    // Arrange
    String key = "key", value = "value";
    byte[] encoded = SegmentImpl.encodeKeyAndValue(key, value);

    SegmentFile mockSegmentFile = mock(SegmentFile.class);
    doReturn((long) encoded.length).when(mockSegmentFile).size();
    when(mockSegmentFile.readByte(anyLong())).thenReturn((byte) 3).thenReturn((byte) 5);
    doReturn(key).when(mockSegmentFile).readAsString(anyInt(), anyLong());

    // Act
    Segment segment = new SegmentImpl(mockSegmentFile);

    // Assert
    assertTrue(segment.containsKey("key"));
  }

  @Test
  void write() throws IOException {
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
  void write_merge() throws IOException {
    try (MockedConstruction<AtomicLong> atomicLongMockedConstruction = mockConstruction(
        AtomicLong.class)) {
      // Arrange
      SegmentFile mockSegmentFile = mock(SegmentFile.class);
      doReturn(0L).when(mockSegmentFile).size();
      doReturn(true).when(mockSegmentFile).isOpen();

      Segment segment = new SegmentImpl(mockSegmentFile);
      AtomicLong mockedAtomicLong = atomicLongMockedConstruction.constructed().get(0);
      /// force merge comparison when writing to simulate another thread having written
      when(mockedAtomicLong.getAndAdd(anyLong())).thenReturn(0L).thenReturn(2L).thenReturn(1L);
      doReturn("value").when(mockSegmentFile).readAsString(anyInt(), anyLong());
      String key = "key", value = "value";

      // Act
      segment.write(key, value);
      segment.write(key, value);
      segment.write(key, value);
      segment.read(key);
      // Assert
      verify(mockSegmentFile, times(1)).readByte(2L + 1 + 3);
    }
  }

  @Test
  void write_Exception() throws IOException {
    String key = "key", value = "value";
    doReturn(true).when(segmentFile).isOpen();
    doThrow(IOException.class).when(segmentFile).write(any(), anyLong());
    assertThrows(IOException.class, () -> segment.write(key, value));
    verify(segmentFile, times(1)).write(any(), anyLong());
  }

  @Test
  void write_afterClose() {
    segment.close();
    assertThrows(RuntimeException.class, () -> segment.write("key", "value"));
  }

  @Test
  void read() throws IOException {
    // Arrange
    String key = "key", value = "value";
    doReturn(true).when(segmentFile).isOpen();
    segment.write(key, value);
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
  void read_exception() throws IOException {
    // Arrange
    String key = "key", value = "value", combined = key + value;
    doReturn(true).when(segmentFile).isOpen();
    segment.write(key, value);
    doThrow(IOException.class).when(segmentFile).readAsString(anyInt(), anyLong());
    // Act
    assertThrows(IOException.class, () -> segment.read(key));
    // Assert
    verify(segmentFile, times(1)).readAsString(anyInt(), anyLong());
  }

  @Test
  void read_keyNotFound() throws IOException {
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
    segment.close();
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
  void containsKey() throws IOException {
    // Arrange
    String key = "key", value = "value";
    assertFalse(segment.containsKey(key));
    doReturn(true).when(segmentFile).isOpen();
    // Act
    segment.write(key, value);
    // Assert
    assertTrue(segment.containsKey(key));
  }

  @Test
  void exceedsStorageThreshold() throws IOException {
    try (MockedConstruction<AtomicLong> atomicLongMockedConstruction = mockConstruction(
        AtomicLong.class)) {
      // Arrange
      SegmentFile mockSegmentFile = mock(SegmentFile.class);
      doReturn(0L).when(mockSegmentFile).size();
      // Act
      Segment segment = new SegmentImpl(mockSegmentFile);
      AtomicLong mockedAtomicLong = atomicLongMockedConstruction.constructed().get(0);
      doReturn(1 + SegmentImpl.NEW_SEGMENT_THRESHOLD).when(mockedAtomicLong).get();
      // Assert
      assertTrue(segment.exceedsStorageThreshold());
    }
  }

  @Test
  void getSegmentKeys() throws IOException {
    // Arrange
    String key = "key", value = "value";
    doReturn(true).when(segmentFile).isOpen();
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
  void delete() throws IOException {
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

}
