package dev.sbutler.bitflask.common.primitives;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.primitives.UnsignedBytes;
import org.junit.jupiter.api.Test;

public class UnsignedShortTest {

  private static final byte[] MIN_VALUE_BYTES = new byte[]{0, 0};
  private static final byte[] MAX_VALUE_BYTES = new byte[]{UnsignedBytes.checkedCast(255),
      UnsignedBytes.checkedCast(255)};


  @Test
  public void value_valid_minValue() {
    UnsignedShort unsignedShort = UnsignedShort.valueOf(UnsignedShort.MIN_VALUE);

    assertThat(unsignedShort.value()).isEqualTo(UnsignedShort.MIN_VALUE);
  }

  @Test
  public void value_valid_maxValue() {
    UnsignedShort unsignedShort = UnsignedShort.valueOf(UnsignedShort.MAX_VALUE);

    assertThat(unsignedShort.value()).isEqualTo(UnsignedShort.MAX_VALUE);
  }

  @Test
  public void value_invalid_lessThanMinValue() {
    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class,
            () -> UnsignedShort.valueOf(UnsignedShort.MIN_VALUE - 1));

    assertThat(e).hasMessageThat().ignoringCase().contains("Provided value less than MIN_VALUE");
  }

  @Test
  public void value_invalid_greaterThanMaxValue() {
    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class,
            () -> UnsignedShort.valueOf(UnsignedShort.MAX_VALUE + 1));

    assertThat(e).hasMessageThat().ignoringCase().contains("Provided value greater than MAX_VALUE");
  }

  @Test
  public void fromBytes_valid_minValue() {
    UnsignedShort unsignedShort = UnsignedShort.fromBytes(MIN_VALUE_BYTES);

    assertThat(unsignedShort.value()).isEqualTo(UnsignedShort.MIN_VALUE);
  }

  @Test
  public void fromBytes_valid_maxValue() {
    UnsignedShort unsignedShort = UnsignedShort.fromBytes(MAX_VALUE_BYTES);

    assertThat(unsignedShort.value()).isEqualTo(UnsignedShort.MAX_VALUE);
  }

  @Test
  public void fromBytes_invalid_arrayLength_lessThanTwo() {
    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class,
            () -> UnsignedShort.fromBytes(new byte[0]));

    assertThat(e).hasMessageThat().ignoringCase().contains("Byte array length must be 2");
  }

  @Test
  public void fromBytes_invalid_arrayLength_greaterThanTwo() {
    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class,
            () -> UnsignedShort.fromBytes(new byte[3]));

    assertThat(e).hasMessageThat().ignoringCase().contains("Byte array length must be 2");
  }

  @Test
  public void toByteArray_valid_minValue() {
    UnsignedShort unsignedShort = UnsignedShort.valueOf(UnsignedShort.MIN_VALUE);

    byte[] bytes = unsignedShort.toByteArray();

    assertThat(bytes).isEqualTo(MIN_VALUE_BYTES);
  }

  @Test
  public void toByteArray_valid_maxValue() {
    UnsignedShort unsignedShort = UnsignedShort.valueOf(UnsignedShort.MAX_VALUE);

    byte[] bytes = unsignedShort.toByteArray();

    assertThat(bytes).isEqualTo(MAX_VALUE_BYTES);
  }
}
