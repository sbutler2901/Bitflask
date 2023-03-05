package dev.sbutler.bitflask.storage.lsm.entry;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.primitives.Bytes;
import dev.sbutler.bitflask.common.primitives.UnsignedShort;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class EntryTest {

  @Test
  public void constructor_negativeEpochSeconds_throwsIllegalArgumentException() {
    long creationEpochSeconds = -1;
    String key = "key";
    String value = "value";

    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class,
            () -> new Entry(creationEpochSeconds, key, value));

    assertThat(e).hasMessageThat().ignoringCase()
        .contains("CreationEpochSeconds cannot be negative.");
  }

  @Test
  public void constructor_emptyKey_throwsIllegalArgumentException() {
    long creationEpochSeconds = Instant.now().getEpochSecond();
    String key = "";
    String value = "value";

    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class,
            () -> new Entry(creationEpochSeconds, key, value));

    assertThat(e).hasMessageThat().ignoringCase()
        .contains("Key must not be empty.");
  }

  @Test
  public void constructor_keyGreaterThanMaxLength_throwsIllegalArgumentException() {
    long creationEpochSeconds = Instant.now().getEpochSecond();
    String key = String.valueOf(new char[Entry.KEY_MAX_LENGTH + 1]);
    String value = "value";

    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class,
            () -> new Entry(creationEpochSeconds, key, value));

    assertThat(e).hasMessageThat().ignoringCase()
        .contains("Key length greater than allowed.");
  }

  @Test
  public void constructor_valueGreaterThanMaxLength_throwsIllegalArgumentException() {
    long creationEpochSeconds = Instant.now().getEpochSecond();
    String key = "key";
    String value = String.valueOf(new char[Entry.VALUE_MAX_LENGTH + 1]);

    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class,
            () -> new Entry(creationEpochSeconds, key, value));

    assertThat(e).hasMessageThat().ignoringCase()
        .contains("Value length greater than allowed.");
  }

  @Test
  public void identityConversion_fromBytes() {
    long creationEpochSeconds = Instant.now().getEpochSecond();
    String key = "key";
    String value = "value";
    EntryMetadata metadata = new EntryMetadata(creationEpochSeconds,
        UnsignedShort.valueOf(key.length()),
        UnsignedShort.valueOf(value.length()));
    byte[] expectedBytes = Bytes.concat(metadata.getBytes(),
        key.getBytes(StandardCharsets.UTF_8),
        value.getBytes(StandardCharsets.UTF_8));

    byte[] bytes = Entry.fromBytes(expectedBytes).getBytes();

    assertThat(bytes).isEqualTo(expectedBytes);
  }

  @Test
  public void identityConversion_getBytes() {
    long creationEpochSeconds = Instant.now().getEpochSecond();
    String key = "key";
    String value = "value";
    Entry expected = new Entry(creationEpochSeconds, key, value);

    Entry created = Entry.fromBytes(expected.getBytes());

    assertThat(created).isEqualTo(expected);
  }

  @Test
  public void fromBytes() {
    long creationEpochSeconds = Instant.now().getEpochSecond();
    String key = "key";
    String value = "value";
    EntryMetadata metadata = new EntryMetadata(creationEpochSeconds,
        UnsignedShort.valueOf(key.length()),
        UnsignedShort.valueOf(value.length()));
    byte[] bytes = Bytes.concat(metadata.getBytes(),
        key.getBytes(StandardCharsets.UTF_8),
        value.getBytes(StandardCharsets.UTF_8));

    Entry entry = Entry.fromBytes(bytes);

    assertThat(entry).isEqualTo(new Entry(creationEpochSeconds, key, value));
  }

  @Test
  public void fromBytes_arrayLengthLessThanMinimum_throwsIllegalArgumentException() {
    byte[] bytes = new byte[Entry.MIN_BYTES - 1];

    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> Entry.fromBytes(bytes));

    assertThat(e).hasMessageThat().ignoringCase().contains("Byte array length invalid.");
  }

  @Test
  public void fromBytes_byteArrayHeaderMismatch_throwsIllegalArgumentException() {
    EntryMetadata metadata = new EntryMetadata(Instant.now().getEpochSecond(),
        UnsignedShort.valueOf(1), UnsignedShort.valueOf(0));
    byte[] keyValueBytes = new byte[Entry.MIN_BYTES + 10];
    byte[] bytes = Bytes.concat(metadata.getBytes(), keyValueBytes);

    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> Entry.fromBytes(bytes));

    assertThat(e).hasMessageThat().ignoringCase()
        .contains("Byte array length does not match decoded header.");
  }

  @Test
  public void getBytes() {
    // Arrange
    long creationEpochSeconds = Instant.now().getEpochSecond();
    String key = "key";
    String value = "value";
    Entry entry = new Entry(creationEpochSeconds, key, value);

    // Act
    byte[] bytes = entry.getBytes();

    // Assert
    byte[] metadataBytes = Arrays.copyOfRange(bytes, 0, EntryMetadata.BYTES);
    EntryMetadata decodedMetadata = EntryMetadata.fromBytes(metadataBytes);
    assertThat(decodedMetadata).isEqualTo(entry.getMetaData());

    String decodedKey = new String(bytes, EntryMetadata.BYTES,
        decodedMetadata.keyLength().value(), StandardCharsets.UTF_8);
    assertThat(decodedKey).isEqualTo(key);

    int valueOffset = EntryMetadata.BYTES + decodedMetadata.keyLength().value();
    String decodedValue = new String(bytes, valueOffset,
        decodedMetadata.valueLength().value(), StandardCharsets.UTF_8);

    assertThat(decodedValue).isEqualTo(value);
  }

  @Test
  public void isDeleted_deletedEntry_returnsTrue() {
    Entry entry = new Entry(Instant.now().getEpochSecond(), "key", "");

    assertThat(entry.isDeleted()).isTrue();
  }

  @Test
  public void isDeleted_presentEntry_returnsFalse() {
    Entry entry = new Entry(Instant.now().getEpochSecond(), "key", "value");

    assertThat(entry.isDeleted()).isFalse();
  }
}
