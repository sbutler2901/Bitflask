package dev.sbutler.bitflask.storage.lsm.entry;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.primitives.Bytes;
import dev.sbutler.bitflask.common.primitives.UnsignedShort;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * A single key:value entry with its creation time in epoch seconds.
 *
 * @param creationEpochSeconds creation time of this entry as the number of seconds since epoch.
 *                             Cannot be negative.
 * @param key                  the entry's key. Cannot be empty or longer than
 *                             {@link #KEY_MAX_LENGTH}
 * @param value                the entry's value. Cannot be longer than {@link #VALUE_MAX_LENGTH}
 */
public record Entry(long creationEpochSeconds, String key, String value) {

  public static final int KEY_MAX_LENGTH = UnsignedShort.MAX_VALUE;
  public static final int VALUE_MAX_LENGTH = UnsignedShort.MAX_VALUE;

  /**
   * The minimum number of bits used to represent an Entry.
   */
  static final int MIN_SIZE = EntryMetadata.SIZE + Byte.SIZE;
  /**
   * The minimum number of bytes to represent an Entry.
   */
  static final int MIN_BYTES = MIN_SIZE / Byte.SIZE;

  public Entry {
    checkArgument(creationEpochSeconds >= 0,
        "CreationEpochSeconds cannot be negative. Provided [%s]", creationEpochSeconds);
    checkArgument(!key.isEmpty(), "Key must not be empty.");
    checkArgument(key.length() <= KEY_MAX_LENGTH,
        "Key length greater than allowed. Provided [%s], max allowed [%s]", key.length(),
        KEY_MAX_LENGTH);
    checkArgument(value.length() <= VALUE_MAX_LENGTH,
        "Value length greater than allowed. Provided [%s], max allowed [%s]", value.length(),
        VALUE_MAX_LENGTH);

  }

  public static Entry fromBytes(byte[] bytes) {
    checkArgument(bytes.length >= MIN_BYTES,
        "Byte array length invalid. Provided [%s], expected at least [%s]",
        bytes.length, MIN_BYTES);

    byte[] metadataBytes = Arrays.copyOfRange(bytes, 0, EntryMetadata.BYTES);
    EntryMetadata decodedMetadata = EntryMetadata.fromBytes(metadataBytes);

    int expectedArrayLength = EntryMetadata.BYTES + decodedMetadata.keyLength().value()
        + decodedMetadata.valueLength().value();
    checkArgument(bytes.length == expectedArrayLength,
        "Byte array length does not match decoded header. Provided [%s], expected [%s]",
        bytes.length,
        expectedArrayLength);

    String decodedKey = new String(bytes, EntryMetadata.BYTES,
        decodedMetadata.keyLength().value(), StandardCharsets.UTF_8);

    int valueOffset = EntryMetadata.BYTES + decodedMetadata.keyLength().value();
    String decodedValue = new String(bytes, valueOffset,
        decodedMetadata.valueLength().value(), StandardCharsets.UTF_8);

    return new Entry(decodedMetadata.creationEpochSeconds(), decodedKey, decodedValue);
  }

  /**
   * Converts the Entry into a byte array.
   *
   * <p>The first 12 bytes will be the {@link EntryMetadata} and encoded according to
   * {@link EntryMetadata#getBytes()}. Following the header will be the key and then the value
   * encoded as {@link StandardCharsets#UTF_8}.
   */
  public byte[] getBytes() {
    EntryMetadata entryMetadata = getMetaData();

    byte[] metadataBytes = entryMetadata.getBytes();
    byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
    byte[] valueBytes = value.getBytes(StandardCharsets.UTF_8);

    return Bytes.concat(metadataBytes, keyBytes, valueBytes);
  }

  /**
   * Returns true if this Entry represents a deleted one.
   */
  public boolean isDeleted() {
    return value.isEmpty();
  }

  /**
   * The number of bytes required to represent this Entry.
   */
  public long getNumBytesSize() {
    return getBytes().length;
  }

  /**
   * Returns the {@link EntryMetadata} for this Entry.
   */
  public EntryMetadata getMetaData() {
    return new EntryMetadata(creationEpochSeconds,
        UnsignedShort.valueOf(key.length()),
        UnsignedShort.valueOf(value.length()));
  }

}
