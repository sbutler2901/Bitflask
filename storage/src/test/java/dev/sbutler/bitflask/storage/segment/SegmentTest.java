package dev.sbutler.bitflask.storage.segment;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
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
    when(segmentFile.isOpen()).thenReturn(true);
    when(keyedEntryMap.containsKey(anyString())).thenReturn(true);
    when(keyedEntryMap.get(anyString())).thenReturn(entry);
    when(segmentFile.readByte(anyLong()))
        .thenReturn(Header.KEY_VALUE.getByteMap())
        .thenReturn((byte) 5);
    when(segmentFile.readAsString(anyInt(), anyLong())).thenReturn(value);
    // Act
    Optional<String> result = segment.read(key);
    // Assert
    assertThat(result).isPresent();
    assertThat(result).hasValue(value);
    verify(segmentFile, times(1)).readAsString(anyInt(), anyLong());
  }

  @Test
  void read_deletedEntry() throws Exception {
    // Arrange
    String key = "key";
    Entry entry = new Entry(Header.DELETED, 0L);
    when(segmentFile.isOpen()).thenReturn(true);
    when(keyedEntryMap.containsKey(anyString())).thenReturn(true);
    when(keyedEntryMap.get(anyString())).thenReturn(entry);
    // Act
    Optional<String> result = segment.read(key);
    // Assert
    assertThat(result).isEmpty();
  }

  @Test
  void read_exception() throws Exception {
    // Arrange
    String key = "key";
    Entry entry = new Entry(Header.KEY_VALUE, 0L);
    when(segmentFile.isOpen()).thenReturn(true);
    when(keyedEntryMap.containsKey(anyString())).thenReturn(true);
    when(keyedEntryMap.get(anyString())).thenReturn(entry);
    when(segmentFile.readByte(anyLong()))
        .thenReturn(Header.KEY_VALUE.getByteMap())
        .thenReturn((byte) 5);
    IOException ioException = new IOException("test");
    when(segmentFile.readAsString(anyInt(), anyLong()))
        .thenThrow(ioException);
    // Act
    IOException exception =
        assertThrows(IOException.class, () -> segment.read(key));
    // Assert
    assertThat(exception).isEqualTo(ioException);
    verify(segmentFile, times(1)).readAsString(anyInt(), anyLong());
  }

  @Test
  void read_keyNotFound() throws Exception {
    // Arrange
    String key = "key";
    when(keyedEntryMap.containsKey(anyString())).thenReturn(false);
    // Act
    Optional<String> readResults = segment.read(key);
    // Assert
    assertThat(readResults).isEmpty();
    verify(segmentFile, times(0)).read(anyInt(), anyLong());
  }

  @Test
  void read_afterClose() {
    // Arrange
    String key = "key";
    when(keyedEntryMap.containsKey(anyString())).thenReturn(true);
    when(segmentFile.isOpen()).thenReturn(false);
    // Act
    SegmentClosedException e =
        assertThrows(SegmentClosedException.class, () -> segment.read(key));
    // Assert
    assertThat(e).hasMessageThat().ignoringCase().contains("closed");
    assertThat(e).hasMessageThat().ignoringCase().contains("read");
  }

  @Test
  void write() throws Exception {
    // Arrange
    String key = "key", value = "value";
    byte[] encoded = Encoder.encode(Header.KEY_VALUE, key, value);
    when(segmentFile.isOpen()).thenReturn(true);
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
    when(segmentFile.isOpen()).thenReturn(true);
    AtomicBoolean wasCalled = new AtomicBoolean(false);
    Consumer<Segment> limitConsumer = (ignored) -> wasCalled.set(true);
    segment.registerSizeLimitExceededConsumer(limitConsumer);
    // Act
    segment.write("key", "value");
    // Assert
    assertThat(wasCalled.get()).isTrue();
  }

  @Test
  void write_Exception() throws Exception {
    // Arrange
    String key = "key", value = "value";
    when(segmentFile.isOpen()).thenReturn(true);
    IOException ioException = new IOException("test");
    doThrow(ioException).when(segmentFile).write(any(), anyLong());
    // Act
    IOException exception =
        assertThrows(IOException.class, () -> segment.write(key, value));
    // Assert
    assertThat(exception).isEqualTo(ioException);
    verify(segmentFile, times(1)).write(any(), anyLong());
  }

  @Test
  void write_afterClose() {
    // Arrange
    when(segmentFile.isOpen()).thenReturn(false);
    // Act
    SegmentClosedException e =
        assertThrows(SegmentClosedException.class, () -> segment.write("key", "value"));
    // Assert
    assertThat(e).hasMessageThat().ignoringCase().contains("closed");
    assertThat(e).hasMessageThat().ignoringCase().contains("written");
  }

  @Test
  void delete() throws Exception {
    // Arrange
    String key = "key";
    byte[] encoded = Encoder.encodeNoValue(Header.DELETED, key);
    Entry entry = new Entry(Header.KEY_VALUE, 0L);
    when(keyedEntryMap.get(anyString())).thenReturn(entry);
    when(segmentFile.isOpen()).thenReturn(true);
    // Act
    segment.delete(key);
    // Assert
    verify(segmentFile, times(1)).write(aryEq(encoded), anyLong());
    verify(keyedEntryMap, times(1)).put(eq(key), any(Entry.class));
  }

  @Test
  void delete_sizeLimitExceededConsumer() throws Exception {
    // Arrange
    String key = "key";
    Entry entry = new Entry(Header.KEY_VALUE, 0L);
    when(keyedEntryMap.get(anyString())).thenReturn(entry);
    when(segmentFile.isOpen()).thenReturn(true);
    segment = new Segment(segmentFile, keyedEntryMap, currentFileWriteOffset, 1);
    AtomicBoolean wasCalled = new AtomicBoolean(false);
    Consumer<Segment> limitConsumer = (ignored) -> wasCalled.set(true);
    segment.registerSizeLimitExceededConsumer(limitConsumer);
    // Act
    segment.delete(key);
    // Assert
    assertThat(wasCalled.get()).isTrue();
  }

  @Test
  void delete_Exception() throws Exception {
    // Arrange
    String key = "key";
    Entry entry = new Entry(Header.KEY_VALUE, 0L);
    when(keyedEntryMap.get(anyString())).thenReturn(entry);
    when(segmentFile.isOpen()).thenReturn(true);
    IOException ioException = new IOException("test");
    doThrow(ioException).when(segmentFile).write(any(), anyLong());
    // Act
    IOException exception =
        assertThrows(IOException.class, () -> segment.delete(key));
    // Assert
    assertThat(exception).isEqualTo(ioException);
    verify(segmentFile, times(1)).write(any(), anyLong());
  }

  @Test
  void delete_afterClose() {
    // Arrange
    String key = "key";
    Entry entry = new Entry(Header.KEY_VALUE, 0L);
    when(keyedEntryMap.get(anyString())).thenReturn(entry);
    when(segmentFile.isOpen()).thenReturn(false);
    // Act
    SegmentClosedException e =
        assertThrows(SegmentClosedException.class, () -> segment.delete(key));
    // Assert
    assertThat(e).hasMessageThat().ignoringCase().contains("closed");
    assertThat(e).hasMessageThat().ignoringCase().contains("deleted");
  }

  @Test
  void containsKey() throws Exception {
    // Arrange
    String key = "key", value = "value";
    assertThat(segment.containsKey(key)).isFalse();
    when(segmentFile.isOpen()).thenReturn(true);
    when(keyedEntryMap.containsKey(anyString())).thenReturn(true);
    // Act
    segment.write(key, value);
    // Assert
    assertThat(segment.containsKey(key)).isTrue();
  }

  @Test
  void exceedsStorageThreshold() throws Exception {
    // Arrange
    segment = new Segment(segmentFile, keyedEntryMap, currentFileWriteOffset, 1);
    when(segmentFile.isOpen()).thenReturn(true);
    segment.write("key", "value");
    // Act / Assert
    assertThat(segment.exceedsStorageThreshold()).isTrue();
  }

  @Test
  void getSegmentKeyHeaderMap() {
    // Arrange
    var keyedEntryMapEntries = ImmutableMap.of(
        "key0",
        new Entry(Header.KEY_VALUE, 0L),
        "key1",
        new Entry(Header.DELETED, 10L)
    );
    when(keyedEntryMap.entrySet()).thenReturn(keyedEntryMapEntries.entrySet());
    // Act
    ImmutableMap<String, Header> keyHeaderMap = segment.getSegmentKeyHeaderMap();
    // Assert
    ImmutableMap<String, Header> expected = ImmutableMap.of(
        "key0",
        Header.KEY_VALUE,
        "key1",
        Header.DELETED
    );
    assertThat(keyHeaderMap).isEqualTo(expected);
  }

  @Test
  void getSegmentFileKey() {
    // Arrange
    int fileKey = 0;
    when(segmentFile.getSegmentFileKey()).thenReturn(fileKey);
    // Act
    int segmentFileKey = segment.getSegmentFileKey();
    // Assert
    assertThat(segmentFileKey).isEqualTo(fileKey);
  }

  @Test
  void close() {
    // Act
    segment.close();
    // Assert
    verify(segmentFile, times(1)).close();
    assertThat(segment.isOpen()).isFalse();
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
  void deleteSegment_withoutClosing() {
    // Arrange
    when(segmentFile.isOpen()).thenReturn(true);
    // Act
    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> segment.deleteSegment());
    // Assert
    assertThat(exception).hasMessageThat().ignoringCase().contains("closed before deleting");
  }

  @Test
  void compactedCheck() {
    assertThat(segment.hasBeenCompacted()).isFalse();
    segment.markCompacted();
    assertThat(segment.hasBeenCompacted()).isTrue();
  }

  @Test
  void toStringTest() {
    assertThat(segment.toString()).isEqualTo("segment-0");
  }

}
