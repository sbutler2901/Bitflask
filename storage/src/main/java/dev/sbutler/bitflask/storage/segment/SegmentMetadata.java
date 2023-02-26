package dev.sbutler.bitflask.storage.segment;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.primitives.Bytes;
import dev.sbutler.bitflask.common.primitives.UnsignedShort;
import java.util.Arrays;

/**
 * The metadata for a single {@link Segment} instance.
 */
record SegmentMetadata(UnsignedShort segmentNumber, UnsignedShort segmentLevel) {

  /**
   * The length of the byte array used to create a SegmentMetadata instance and when an instance is
   * converted into a byte array.
   */
  static final int BYTE_ARRAY_LENGTH = 4;

  /**
   * Creates a new SegmentMetadata instance from the provided byte array.
   *
   * <p>The 0th and 1st indices will be interpreted as a 16-bit unsigned short representing the
   * segmentNumber. The 3rd and 4th indices will be interpreted as a 16-bit unsigned short
   * representing the segmentLevel.
   *
   * <p>An {@link IllegalArgumentException} will be thrown if the provided byte array's length is
   * not 4.
   */
  static SegmentMetadata fromBytes(byte[] bytes) {
    checkArgument(bytes.length == BYTE_ARRAY_LENGTH,
        "Byte array length invalid. Provided [%s], expected [%s]",
        bytes.length, BYTE_ARRAY_LENGTH);

    byte[] segmentNumberBytes = Arrays.copyOfRange(bytes, 0, 2);
    byte[] segmentLevelBytes = Arrays.copyOfRange(bytes, 2, 4);
    UnsignedShort segmentNumber = UnsignedShort.fromBytes(segmentNumberBytes);
    UnsignedShort segmentLevel = UnsignedShort.fromBytes(segmentLevelBytes);
    return new SegmentMetadata(segmentNumber, segmentLevel);
  }

  /**
   * Converts the metadata into a byte array with the segmentNumber in the first 2 indices of the
   * array and segmentLevel in the last two.
   */
  byte[] getBytes() {
    return Bytes.concat(segmentNumber.getBytes(), segmentLevel.getBytes());
  }
}
