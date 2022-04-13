package dev.sbutler.bitflask.storage.segment;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SegmentImplTest {

  @InjectMocks
  SegmentImpl segment;
  @Mock
  SegmentFile segmentFile;

  @Test
  void encodedKeyAndValue() {
    String key = "key", value = "value";
    char keyLengthEncoded = (char) key.length();
    char valueLengthEncoded = (char) value.length();
    byte[] expected = (keyLengthEncoded + key + valueLengthEncoded + value).getBytes();
    byte[] encoded = SegmentImpl.encodeKeyAndValue(key, value);
    assertArrayEquals(expected, encoded);
  }

  @Test
  void encodedKeyAndValue_IllegalArgumentException() {
    String key = new String(new byte[257]);
    String value = new String(new byte[257]);
    assertThrows(IllegalArgumentException.class, () -> SegmentImpl.encodeKeyAndValue(key, "value"));
    assertThrows(IllegalArgumentException.class, () -> SegmentImpl.encodeKeyAndValue("key", value));
  }

  @Test
  void write() throws IOException {
    String key = "key", value = "value";

    segment.write(key, value);
    byte[] encoded = SegmentImpl.encodeKeyAndValue(key, value);
    verify(segmentFile, times(1)).write(aryEq(encoded), anyLong());
  }

  @Test
  void write_Exception() throws IOException {
    String key = "key", value = "value";

    doThrow(IOException.class).when(segmentFile).write(any(), anyLong());
    segment.write(key, value);
    verify(segmentFile, times(1)).write(any(), anyLong());
  }

  @Test
  void read() throws IOException {
    String key = "key", value = "value";
    // write before reading
    segment.write(key, value);

    // read
    doReturn((byte) 5).when(segmentFile).readByte(anyLong());
    doReturn(value).when(segmentFile).readAsString(anyInt(), anyLong());

    Optional<String> result = segment.read(key);

    assertTrue(result.isPresent());
    assertEquals(value, result.get());
    verify(segmentFile, times(1)).readAsString(anyInt(), anyLong());
  }

  @Test
  void read_exception() throws IOException {
    String key = "key", value = "value", combined = key + value;

    // write before reading
    segment.write(key, value);

    // read
    doThrow(IOException.class).when(segmentFile).readAsString(anyInt(), anyLong());

    Optional<String> result = segment.read(key);

    assertTrue(result.isEmpty());
    verify(segmentFile, times(1)).readAsString(anyInt(), anyLong());
  }

  @Test
  void read_keyNotFound() throws IOException {
    String key = "key";

    assertTrue(segment.read(key).isEmpty());
    verify(segmentFile, times(0)).read(anyInt(), anyLong());
  }

  @Test
  void exceedsStorageThreshold() throws IOException {
    try (MockedConstruction<AtomicLong> atomicLongMockedConstruction = mockConstruction(
        AtomicLong.class)) {
      SegmentFile mockSegmentFile = mock(SegmentFile.class);
      doReturn(0L).when(mockSegmentFile).size();

      Segment segment = new SegmentImpl(mockSegmentFile);

      AtomicLong mockedAtomicLong = atomicLongMockedConstruction.constructed().get(0);
      doReturn(1 + SegmentImpl.NEW_SEGMENT_THRESHOLD).when(mockedAtomicLong).get();

      assertTrue(segment.exceedsStorageThreshold());
    }
  }

  @Test
  void loadFileEntries() throws IOException {
    String key = "key", value = "value";
    byte[] encoded = SegmentImpl.encodeKeyAndValue(key, value);

    SegmentFile mockSegmentFile = mock(SegmentFile.class);
    doReturn((long) encoded.length).when(mockSegmentFile).size();
    when(mockSegmentFile.readByte(anyLong())).thenReturn((byte) 3).thenReturn((byte) 5);
    doReturn(key).when(mockSegmentFile).readAsString(anyInt(), anyLong());

    Segment segment = new SegmentImpl(mockSegmentFile);

    assertTrue(segment.containsKey("key"));
  }

}
