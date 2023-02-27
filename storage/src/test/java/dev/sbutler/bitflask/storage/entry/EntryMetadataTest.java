package dev.sbutler.bitflask.storage.entry;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import dev.sbutler.bitflask.common.primitives.UnsignedShort;
import java.time.Instant;
import org.junit.jupiter.api.Test;

public class EntryMetadataTest {

  @Test
  public void identityConversion_fromBytes() {
    long creationEpochSeconds = Instant.now().getEpochSecond();
    UnsignedShort keyLength = UnsignedShort.valueOf(UnsignedShort.MAX_VALUE);
    UnsignedShort valueLength = UnsignedShort.valueOf(UnsignedShort.MIN_VALUE);
    byte[] expectedBytes = Bytes.concat(
        Longs.toByteArray(creationEpochSeconds),
        keyLength.getBytes(),
        valueLength.getBytes());

    byte[] bytes = EntryMetadata.fromBytes(expectedBytes).getBytes();

    assertThat(bytes).isEqualTo(expectedBytes);
  }

  @Test
  public void identityConversion_getBytes() {
    long creationEpochSeconds = Instant.now().getEpochSecond();
    UnsignedShort keyLength = UnsignedShort.valueOf(UnsignedShort.MAX_VALUE);
    UnsignedShort valueLength = UnsignedShort.valueOf(UnsignedShort.MIN_VALUE);
    EntryMetadata expected = new EntryMetadata(creationEpochSeconds, keyLength, valueLength);

    EntryMetadata created = EntryMetadata.fromBytes(expected.getBytes());

    assertThat(created).isEqualTo(expected);
  }

  @Test
  public void fromBytes_valid() {
    long creationEpochSeconds = Instant.now().getEpochSecond();
    UnsignedShort keyLength = UnsignedShort.valueOf(UnsignedShort.MAX_VALUE);
    UnsignedShort valueLength = UnsignedShort.valueOf(UnsignedShort.MIN_VALUE);
    byte[] bytes = Bytes.concat(
        Longs.toByteArray(creationEpochSeconds),
        keyLength.getBytes(),
        valueLength.getBytes());

    EntryMetadata metadata = EntryMetadata.fromBytes(bytes);

    assertThat(metadata.creationEpochSeconds()).isEqualTo(creationEpochSeconds);
    assertThat(metadata.getKeyLength()).isEqualTo(keyLength.value());
    assertThat(metadata.getValueLength()).isEqualTo(valueLength.value());
  }

  @Test
  public void fromBytes_invalid_negativeCreationEpochSeconds_throwsIllegalArgumentException() {
    long creationEpochSeconds = -1;
    UnsignedShort keyLength = UnsignedShort.valueOf(UnsignedShort.MAX_VALUE);
    UnsignedShort valueLength = UnsignedShort.valueOf(UnsignedShort.MIN_VALUE);
    byte[] bytes = Bytes.concat(
        Longs.toByteArray(creationEpochSeconds),
        keyLength.getBytes(),
        valueLength.getBytes());

    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> EntryMetadata.fromBytes(bytes));

    assertThat(e).hasMessageThat().ignoringCase()
        .contains("CreationEpochSeconds cannot be negative.");
  }

  @Test
  public void fromBytes_invalid_zeroKeyLength_throwsIllegalArgumentException() {
    long creationEpochSeconds = Instant.now().getEpochSecond();
    UnsignedShort keyLength = UnsignedShort.valueOf(UnsignedShort.MIN_VALUE);
    UnsignedShort valueLength = UnsignedShort.valueOf(UnsignedShort.MIN_VALUE);
    byte[] bytes = Bytes.concat(
        Longs.toByteArray(creationEpochSeconds),
        keyLength.getBytes(),
        valueLength.getBytes());

    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> EntryMetadata.fromBytes(bytes));

    assertThat(e).hasMessageThat().ignoringCase()
        .contains("Key length must be greater than 0");
  }

  @Test
  public void fromBytes_invalidLength_lessThan_throwsIllegalArgumentException() {
    byte[] bytes = new byte[]{EntryMetadata.BYTE_ARRAY_LENGTH - 1};

    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> EntryMetadata.fromBytes(bytes));

    assertThat(e).hasMessageThat().ignoringCase().contains("Byte array length invalid.");
  }

  @Test
  public void fromBytes_invalidLength_greaterThan_throwsIllegalArgumentException() {
    byte[] bytes = new byte[]{EntryMetadata.BYTE_ARRAY_LENGTH + 1};

    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> EntryMetadata.fromBytes(bytes));

    assertThat(e).hasMessageThat().ignoringCase().contains("Byte array length invalid.");
  }

  @Test
  public void getBytes() {
    long creationEpochSeconds = Instant.now().getEpochSecond();
    UnsignedShort keyLength = UnsignedShort.valueOf(UnsignedShort.MAX_VALUE);
    UnsignedShort valueLength = UnsignedShort.valueOf(UnsignedShort.MIN_VALUE);
    EntryMetadata metadata = new EntryMetadata(creationEpochSeconds, keyLength, valueLength);
    byte[] expectedBytes = Bytes.concat(
        Longs.toByteArray(creationEpochSeconds),
        keyLength.getBytes(),
        valueLength.getBytes());

    byte[] bytes = metadata.getBytes();

    assertThat(bytes).isEqualTo(expectedBytes);
  }
}
