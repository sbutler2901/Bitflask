package dev.sbutler.bitflask.common.primitives;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.primitives.UnsignedBytes;

/**
 * Represents a 2-byte unsigned value.
 *
 * <p>capable of representing the inclusive number range 0 - 65,535.
 *
 * <p>Java's {@code short} primitive can only support the inclusive number range 32,768 - 32,767.
 * As a result the value must be received as an {@code int} to represent the range supported by this
 * class.
 */
public record UnsignedShort(int value) {

  /**
   * The number of bits used to represent an UnsignedShort.
   */
  public static final int SIZE = 16;
  /**
   * The number of bytes to represent and UnsignedShort.
   */
  public static final int BYTES = SIZE / Byte.SIZE;
  /**
   * A constant holding the maximum value an UnsignedShort can have, 2^16-1.
   */
  public static final int MAX_VALUE = 65535;
  /**
   * A constant holding the minimum value an UnsignedShort can have, 0.
   */
  public static final int MIN_VALUE = 0;

  private static final int HIGH_ORDER_BYTE_BITMASK = 0xff00;
  private static final int LOWER_ORDER_BYTE_BITMASK = 0x00ff;


  public UnsignedShort {
    checkArgument(value >= MIN_VALUE,
        "Provided value less than MIN_VALUE. Received [%d], expected [%d]", value, MIN_VALUE);
    checkArgument(value <= MAX_VALUE,
        "Provided value greater than MAX_VALUE. Received [%d], expected [%d]", value, MAX_VALUE);
  }

  /**
   * Expects a value in the inclusive number range 0 - 65,535
   *
   * <p>An {@link IllegalArgumentException} will be thrown if an invalid value is provided.
   */
  public static UnsignedShort valueOf(int value) {
    return new UnsignedShort(value);
  }

  /**
   * Creates a new UnsignedShort instant from the provided byte array.
   *
   * <p>The 0th index of the array will be interpreted as the higher order byte and the 1st index
   * will be interpreted as the lower order byte.
   *
   * <p>An {@link IllegalArgumentException} will be thrown if the provided byte array's length is
   * not 2.
   */
  public static UnsignedShort fromBytes(byte[] bytes) {
    checkArgument(bytes.length == 2, "Byte array length must be 2. Provided array's length [%d]",
        bytes.length);
    int higherOrderShifted = (UnsignedBytes.toInt(bytes[0]) << Byte.SIZE);
    int value = higherOrderShifted + UnsignedBytes.toInt(bytes[1]);
    return new UnsignedShort(value);
  }

  /**
   * Converts the stored value into a byte array of length 2.
   *
   * <p>The higher ordered byte will be in the 0th index with the lower ordered byte in the 1st
   * index.
   */
  public byte[] toByteArray() {
    int higherOrderByteMasked = (value & HIGH_ORDER_BYTE_BITMASK) >> Byte.SIZE;
    int lowerOrderByteMasked = value & LOWER_ORDER_BYTE_BITMASK;
    byte higherOrderByte = UnsignedBytes.checkedCast(higherOrderByteMasked);
    byte lowerOrderByte = UnsignedBytes.checkedCast(lowerOrderByteMasked);
    return new byte[]{higherOrderByte, lowerOrderByte};
  }
}
