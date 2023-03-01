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
  public void identityConversion_fromBytes_minValue() {
    byte[] bytes = UnsignedShort.fromBytes(MIN_VALUE_BYTES).getBytes();

    assertThat(bytes).isEqualTo(MIN_VALUE_BYTES);
  }

  @Test
  public void identityConversion_fromBytes_maxValue() {
    byte[] bytes = UnsignedShort.fromBytes(MAX_VALUE_BYTES).getBytes();

    assertThat(bytes).isEqualTo(MAX_VALUE_BYTES);
  }

  @Test
  public void identityConversion_getBytes_minValue() {
    UnsignedShort expected = new UnsignedShort(UnsignedShort.MIN_VALUE);

    UnsignedShort created = UnsignedShort.fromBytes(expected.getBytes());

    assertThat(created).isEqualTo(expected);
  }

  @Test
  public void identityConversion_getBytes_maxValue() {
    UnsignedShort expected = new UnsignedShort(UnsignedShort.MAX_VALUE);

    UnsignedShort created = UnsignedShort.fromBytes(expected.getBytes());

    assertThat(created).isEqualTo(expected);
  }

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
  public void value_invalid_lessThanMinValue_throwsIllegalArgumentException() {
    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class,
            () -> UnsignedShort.valueOf(UnsignedShort.MIN_VALUE - 1));

    assertThat(e).hasMessageThat().ignoringCase().contains("Provided value less than MIN_VALUE");
  }

  @Test
  public void value_invalid_greaterThanMaxValue_throwsIllegalArgumentException() {
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
  public void fromBytes_invalid_arrayLength_lessThanTwo_throwsIllegalArgumentException() {
    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class,
            () -> UnsignedShort.fromBytes(new byte[0]));

    assertThat(e).hasMessageThat().ignoringCase().contains("Byte array length invalid.");
  }

  @Test
  public void fromBytes_invalid_arrayLength_greaterThanTwo_throwsIllegalArgumentException() {
    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class,
            () -> UnsignedShort.fromBytes(new byte[3]));

    assertThat(e).hasMessageThat().ignoringCase().contains("Byte array length invalid.");
  }

  @Test
  public void getBytes_valid_minValue() {
    UnsignedShort unsignedShort = UnsignedShort.valueOf(UnsignedShort.MIN_VALUE);

    byte[] bytes = unsignedShort.getBytes();

    assertThat(bytes).isEqualTo(MIN_VALUE_BYTES);
  }

  @Test
  public void getBytes_valid_maxValue() {
    UnsignedShort unsignedShort = UnsignedShort.valueOf(UnsignedShort.MAX_VALUE);

    byte[] bytes = unsignedShort.getBytes();

    assertThat(bytes).isEqualTo(MAX_VALUE_BYTES);
  }
}
