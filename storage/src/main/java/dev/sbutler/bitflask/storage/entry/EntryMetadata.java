package dev.sbutler.bitflask.storage.entry;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import dev.sbutler.bitflask.common.primitives.UnsignedShort;
import java.util.Arrays;

/**
 * The metadata for a single {@link Entry}.
 *
 * <p>The {@code creationEpochSeconds} cannot be negative and the {@code keyLength} must be
 * non-empty.
 */
public record EntryMetadata(long creationEpochSeconds, UnsignedShort keyLength,
                            UnsignedShort valueLength) {

  /**
   * The length of the byte array used to create a SegmentEntryMetadata instance and when an
   * instance is converted into a byte array.
   */
  public static final int BYTE_ARRAY_LENGTH = 12;

  public EntryMetadata {
    checkArgument(creationEpochSeconds >= 0,
        "CreationEpochSeconds cannot be negative. Provided [%s]", creationEpochSeconds);
    checkArgument(keyLength.value() > 0, "Key length must be greater than 0");
  }

  /**
   * Creates a new EntryMetadata instance from the provided byte array.
   *
   * <p>The first 8 indices will be interpreted as an 8-byte long representing the
   * creationEpochSeconds. The next two indices will be interpreted as a 2-byte unsigned short
   * representing the key length. The final two indices will be interpreted as the value length.
   *
   * <p>An {@link IllegalArgumentException} will be thrown if the provided byte array's length is
   * not 12.
   */
  public static EntryMetadata fromBytes(byte[] bytes) {
    checkArgument(bytes.length == BYTE_ARRAY_LENGTH,
        "Byte array length invalid. Provided [%s], expected [%s]",
        bytes.length, BYTE_ARRAY_LENGTH);

    byte[] creationEpochSecondsBytes = Arrays.copyOfRange(bytes, 0, 8);
    byte[] keyLengthBytes = Arrays.copyOfRange(bytes, 8, 10);
    byte[] valueLengthBytes = Arrays.copyOfRange(bytes, 10, 12);

    long creationEpochSeconds = Longs.fromByteArray(creationEpochSecondsBytes);
    UnsignedShort keyLength = UnsignedShort.fromBytes(keyLengthBytes);
    UnsignedShort valueLength = UnsignedShort.fromBytes(valueLengthBytes);

    return new EntryMetadata(creationEpochSeconds, keyLength, valueLength);
  }

  /**
   * Converts the EntryMetadata into a byte array with the creationEpochSeconds in the first 8
   * indices, the key length in the next two, and the value length in the final two.
   */
  public byte[] getBytes() {
    byte[] creationEpochSecondsBytes = Longs.toByteArray(creationEpochSeconds);
    byte[] keyLengthBytes = keyLength.getBytes();
    byte[] valueLengthBytes = valueLength.getBytes();

    return Bytes.concat(creationEpochSecondsBytes, keyLengthBytes, valueLengthBytes);
  }

  public int getKeyLength() {
    return keyLength.value();
  }

  public int getValueLength() {
    return valueLength.value();
  }
}
