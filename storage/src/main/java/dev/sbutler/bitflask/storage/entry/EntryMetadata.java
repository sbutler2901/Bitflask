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
   * The number of bits used to represent a EntryMetadata.
   */
  static final int SIZE = Long.SIZE + UnsignedShort.SIZE * 2;
  /**
   * The number of bytes to represent a SegmentMetadata.
   */
  static final int BYTES = SIZE / Byte.SIZE;

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
    checkArgument(bytes.length == BYTES,
        "Byte array length invalid. Provided [%s], expected [%s]",
        bytes.length, BYTES);

    int keyLengthOffsetEnd = Long.BYTES + UnsignedShort.BYTES;
    byte[] creationEpochSecondsBytes = Arrays.copyOfRange(bytes, 0, Long.BYTES);
    byte[] keyLengthBytes = Arrays.copyOfRange(bytes, Long.BYTES, keyLengthOffsetEnd);
    byte[] valueLengthBytes = Arrays.copyOfRange(bytes, keyLengthOffsetEnd,
        keyLengthOffsetEnd + UnsignedShort.BYTES);

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
