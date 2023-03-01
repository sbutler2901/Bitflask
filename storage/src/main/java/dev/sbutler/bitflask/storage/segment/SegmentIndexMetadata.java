package dev.sbutler.bitflask.storage.segment;

import static com.google.common.base.Preconditions.checkArgument;

import dev.sbutler.bitflask.common.primitives.UnsignedShort;

/**
 * The metadata for a single {@link SegmentIndex} instance.
 *
 * @param segmentNumber the number of the {@link Segment} that the SegmentIndex will correspond
 *                      with.
 */
record SegmentIndexMetadata(UnsignedShort segmentNumber) {

  /**
   * The number of bits used to represent a SegmentMetadata.
   */
  static final int SIZE = UnsignedShort.SIZE;
  /**
   * The number of bytes to represent a SegmentMetadata.
   */
  static final int BYTES = SIZE / Byte.SIZE;

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
    checkArgument(bytes.length == BYTES,
        "Byte array length invalid. Provided [%s], expected [%s]",
        bytes.length, BYTES);

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
