package dev.sbutler.bitflask.storage.segment;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
  void decodedValue() {
    String key = "key", value = "value";
    char keyLengthEncoded = (char) key.length();
    char valueLengthEncoded = (char) value.length();
    byte[] encoded = (keyLengthEncoded + key + valueLengthEncoded + value).getBytes();
    assertEquals(value, SegmentImpl.decodeValue(encoded, key.length()));
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
    doReturn(value.getBytes()).when(segmentFile).read(anyInt(), anyLong());

    Optional<String> result = segment.read(key);

    assertTrue(result.isPresent());
    assertEquals(value, result.get());
    verify(segmentFile, times(1)).read(anyInt(), anyLong());
  }

  @Test
  void read_exception() throws IOException {
    String key = "key", value = "value", combined = key + value;

    // write before reading
    segment.write(key, value);

    // read
    doThrow(IOException.class).when(segmentFile).read(anyInt(), anyLong());

    Optional<String> result = segment.read(key);

    assertTrue(result.isEmpty());
    verify(segmentFile, times(1)).read(anyInt(), anyLong());
  }

  @Test
  void read_keyNotFound() throws IOException {
    String key = "key";

    assertTrue(segment.read(key).isEmpty());
    verify(segmentFile, times(0)).read(anyInt(), anyLong());
  }

  @Test
  void exceedsStorageThreshold() {
    assertFalse(segment.exceedsStorageThreshold());
    // key bytes + 1MiB from value to exceed
    int oneMiB = 1048576;
    String thresholdSizedValue = new String(new char[oneMiB]);
    segment.write("key", thresholdSizedValue);
    assertTrue(segment.exceedsStorageThreshold());
  }

  @Test
  void initialize() throws IOException {
    String key = "key", value = "value";
    byte[] encoded = SegmentImpl.encodeKeyAndValue(key, value);

    SegmentFile mockSegmentFile = mock(SegmentFile.class);
    doReturn((long) encoded.length).when(mockSegmentFile).size();
    when(mockSegmentFile.readByte(anyLong())).thenReturn((byte) 3).thenReturn((byte) 5);
    doReturn(key.getBytes()).when(mockSegmentFile).read(anyInt(), anyLong());

    Segment segment = new SegmentImpl(mockSegmentFile);

    assertTrue(segment.containsKey("key"));
  }

}
