package dev.sbutler.bitflask.storage.lsm.segment;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import dev.sbutler.bitflask.common.primitives.UnsignedShort;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class SegmentIndexEntryTest {

  @Test
  public void constructor_emptyKey_throwsIllegalArgumentException() {
    String key = "";
    long offset = 0L;

    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> new SegmentIndexEntry(key, offset));

    assertThat(e).hasMessageThat().ignoringCase().contains("Key must not be empty.");
  }

  @Test
  public void identityConversion_fromBytes() {
    String key = "key";
    long offset = 0L;
    byte[] expectedBytes = Bytes.concat(
        UnsignedShort.valueOf(key.length()).getBytes(),
        Longs.toByteArray(offset),
        key.getBytes(StandardCharsets.UTF_8));

    byte[] bytes = SegmentIndexEntry.fromBytes(expectedBytes).getBytes();

    assertThat(bytes).isEqualTo(expectedBytes);
  }

  @Test
  public void identityConversion_getBytes() {
    SegmentIndexEntry expected = new SegmentIndexEntry("key", 0L);

    SegmentIndexEntry created = SegmentIndexEntry.fromBytes(expected.getBytes());

    assertThat(created).isEqualTo(expected);
  }

  @Test
  public void fromBytes_valid() {
    String key = "key";
    long offset = 0L;
    byte[] bytes = Bytes.concat(
        UnsignedShort.valueOf(key.length()).getBytes(),
        Longs.toByteArray(offset),
        key.getBytes(StandardCharsets.UTF_8));

    SegmentIndexEntry entry = SegmentIndexEntry.fromBytes(bytes);

    assertThat(entry.key()).isEqualTo(key);
    assertThat(entry.offset()).isEqualTo(offset);
  }

  @Test
  public void fromBytes_invalidLength_lessThan_throwsIllegalArgumentException() {
    byte[] bytes = new byte[SegmentIndexEntry.MIN_BYTES - 1];

    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> SegmentIndexEntry.fromBytes(bytes));

    assertThat(e).hasMessageThat().ignoringCase().contains("Byte array length invalid.");
  }

  @Test
  public void fromBytes_invalid_keyLength_tooLarge_throwsIllegalArgumentException() {
    long offset = 0L;
    byte[] bytes = Bytes.concat(
        new byte[UnsignedShort.BYTES],
        Longs.toByteArray(offset),
        new byte[1]);

    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> SegmentIndexEntry.fromBytes(bytes));

    assertThat(e).hasMessageThat().ignoringCase().contains("Key must not be empty.");
  }

  @Test
  public void fromBytes_invalid_offset_negative_throwsIllegalArgumentException() {
    String key = "key";
    long offset = -1L;
    byte[] bytes = Bytes.concat(
        UnsignedShort.valueOf(key.length()).getBytes(),
        Longs.toByteArray(offset),
        key.getBytes(StandardCharsets.UTF_8));

    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> SegmentIndexEntry.fromBytes(bytes));

    assertThat(e).hasMessageThat().ignoringCase().contains("Offset negative.");
  }

  @Test
  public void getBytes_valid() {
    String key = "key";
    long offset = 0L;
    SegmentIndexEntry entry = new SegmentIndexEntry(key, offset);
    byte[] expectedBytes = Bytes.concat(
        UnsignedShort.valueOf(key.length()).getBytes(),
        Longs.toByteArray(offset),
        key.getBytes(StandardCharsets.UTF_8));

    byte[] bytes = entry.getBytes();

    assertThat(bytes).isEqualTo(expectedBytes);
  }
}
