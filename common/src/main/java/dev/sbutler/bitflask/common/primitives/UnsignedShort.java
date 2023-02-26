package dev.sbutler.bitflask.common.primitives;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.primitives.Ints;
import java.nio.ByteBuffer;

/**
 * Represents a 2-byte unsigned value.
 *
 * <p>capable of representing the inclusive number range 0 - 65,535.
 *
 * <p>Java's {@code short} primitive can only support the inclusive number range 32,768 - 32,767.
 * As a result the value must be received as an {@code int} to represent the range supported by this
 * class.
 */
public final class UnsignedShort {

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
  /**
   * The expected array length when creating an UnsignedShort from a {@code byte[]}.
   */
  public static final int BYTE_ARRAY_LENGTH = 4;

  private final int value;

  private UnsignedShort(int value) {
    checkArgument(value >= MIN_VALUE,
        "Provided value less than MIN_VALUE. Received [%d], expected [%d]", value, MIN_VALUE);
    checkArgument(value <= MAX_VALUE,
        "Provided value greater than MAX_VALUE. Received [%d], expected [%d]", value, MAX_VALUE);
    this.value = value;
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
   * <p>The provided array must be of length 4.
   *
   * <p>The array will be converted into a value using {@code ByteBuffer.wrap(bytes).getInt()} and
   * must be within the inclusive number range 0 - 65,535.
   *
   * <p>An {@link IllegalArgumentException} will be thrown if the byte array resolves to an invalid
   * value.
   */
  public static UnsignedShort fromBytes(byte[] bytes) {
    checkArgument(bytes.length == BYTE_ARRAY_LENGTH,
        "Provided byte array length invalid. Provided [%d], expected [%d]", bytes.length,
        BYTE_ARRAY_LENGTH);
    int value = ByteBuffer.wrap(bytes).getInt();
    return new UnsignedShort(value);
  }

  /**
   * Gets the value.
   */
  public int getValue() {
    return value;
  }

  /**
   * Converts the stored value into a byte array of length 4.
   */
  public byte[] toByteArray() {
    return Ints.toByteArray(value);
  }
}
