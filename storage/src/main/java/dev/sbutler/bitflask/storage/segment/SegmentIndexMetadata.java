package dev.sbutler.bitflask.storage.segment;

import static com.google.common.base.Preconditions.checkArgument;

import dev.sbutler.bitflask.common.primitives.UnsignedShort;

/**
 * The metadata for a single {@link SegmentIndex} instance.
 */
record SegmentIndexMetadata(UnsignedShort segmentNumber) {

  /**
   * The length of the byte array used to create a SegmentMetadata instance and when an instance is
   * converted into a byte array.
   */
  static final int BYTE_ARRAY_LENGTH = 2;

  /**
   * Creates a new SegmentIndexMetadata instance from the provided byte array.
   *
   * <p>The 0th and 1st indices will be interpreted as a 16-bit unsigned short representing the
   * segmentNumber.
   *
   * <p>An {@link IllegalArgumentException} will be thrown if the provided byte array's length if
   * not 2.
   */
  static SegmentIndexMetadata fromBytes(byte[] bytes) {
    checkArgument(bytes.length == BYTE_ARRAY_LENGTH,
        "Byte array length invalid. Provided [%s], expected [%s]",
        bytes.length, BYTE_ARRAY_LENGTH);
    UnsignedShort segmentNumber = UnsignedShort.fromBytes(bytes);
    return new SegmentIndexMetadata(segmentNumber);
  }

  /**
   * Converts the metadata into a byte array with the segmentNumber in the 2 indices of the array.
   */
  byte[] getBytes() {
    return segmentNumber.getBytes();
  }

  int getSegmentNumber() {
    return segmentNumber.value();
  }
}
