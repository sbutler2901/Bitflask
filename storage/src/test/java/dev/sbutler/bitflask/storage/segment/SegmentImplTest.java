package dev.sbutler.bitflask.storage.segment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import dev.sbutler.bitflask.storage.segment.SegmentImpl.EntryImpl;
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
    byte[] encoded = SegmentImpl.encodeKeyAndValue(key, value);

    // write before reading
    segment.write(key, value);

    // read
    doReturn(encoded).when(segmentFile).read(anyInt(), anyLong());

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

  public static class EntryImplTest {

    @Test
    void entry_invalidArgs() {
      assertThrows(IllegalArgumentException.class, () -> new EntryImpl(-1, 0, 10));
      assertThrows(IllegalArgumentException.class, () -> new EntryImpl(0, -1, 10));
      assertThrows(IllegalArgumentException.class, () -> new EntryImpl(0, 0, 0));
    }

    @Test
    void entry_getters() {
      Segment.Entry entry = new EntryImpl(0, 5, 10);
      assertEquals(0, entry.getSegmentFileOffset());
      assertEquals(5, entry.getKeyLength());
      assertEquals(10, entry.getValueLength());
      assertEquals(15, entry.getTotalLength());
    }

    @Test
    void entry_toString() {
      Segment.Entry entry = new EntryImpl(0, 5, 10);
      String expected = "Entry{segmentOffset=0, keyLength=5, valueLength=10}";
      assertEquals(expected, entry.toString());
    }
  }

}
