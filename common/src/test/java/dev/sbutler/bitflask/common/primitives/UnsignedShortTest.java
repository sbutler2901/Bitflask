package dev.sbutler.bitflask.common.primitives;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.primitives.Ints;
import org.junit.jupiter.api.Test;

public class UnsignedShortTest {

  @Test
  public void value_valid_minValue() {
    UnsignedShort unsignedShort = new UnsignedShort(UnsignedShort.MIN_VALUE);

    assertThat(unsignedShort.value()).isEqualTo(UnsignedShort.MIN_VALUE);
  }

  @Test
  public void value_valid_maxValue() {
    UnsignedShort unsignedShort = new UnsignedShort(UnsignedShort.MAX_VALUE);

    assertThat(unsignedShort.value()).isEqualTo(UnsignedShort.MAX_VALUE);
  }

  @Test
  public void value_invalid_lessThanMinValue() {
    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class,
            () -> new UnsignedShort(UnsignedShort.MIN_VALUE - 1));

    assertThat(e).hasMessageThat().ignoringCase().contains("Provided value less than MIN_VALUE");
  }

  @Test
  public void value_invalid_greaterThanMaxValue() {
    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class,
            () -> new UnsignedShort(UnsignedShort.MAX_VALUE + 1));

    assertThat(e).hasMessageThat().ignoringCase().contains("Provided value greater than MAX_VALUE");
  }

  @Test
  public void fromBytes_valid_minValue() {
    byte[] bytes = Ints.toByteArray(UnsignedShort.MIN_VALUE);

    UnsignedShort unsignedShort = UnsignedShort.fromBytes(bytes);

    assertThat(unsignedShort.value()).isEqualTo(UnsignedShort.MIN_VALUE);
  }

  @Test
  public void fromBytes_valid_maxValue() {
    byte[] bytes = Ints.toByteArray(UnsignedShort.MAX_VALUE);

    UnsignedShort unsignedShort = UnsignedShort.fromBytes(bytes);

    assertThat(unsignedShort.value()).isEqualTo(UnsignedShort.MAX_VALUE);
  }

  @Test
  public void fromBytes_invalid_lessThanMinValue() {
    byte[] bytes = Ints.toByteArray(UnsignedShort.MIN_VALUE - 1);

    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class,
            () -> UnsignedShort.fromBytes(bytes));

    assertThat(e).hasMessageThat().ignoringCase().contains("Provided value less than MIN_VALUE");
  }

  @Test
  public void fromBytes_invalid_greaterThanMaxValue() {
    byte[] bytes = Ints.toByteArray(UnsignedShort.MAX_VALUE + 1);

    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class,
            () -> UnsignedShort.fromBytes(bytes));

    assertThat(e).hasMessageThat().ignoringCase().contains("Provided value greater than MAX_VALUE");
  }

  @Test
  public void fromBytes_invalid_arrayLength() {
    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class,
            () -> UnsignedShort.fromBytes(new byte[0]));

    assertThat(e).hasMessageThat().ignoringCase().contains("Provided byte array length invalid");
  }

  @Test
  public void toByteArray_valid_minValue() {
    UnsignedShort unsignedShort = new UnsignedShort(UnsignedShort.MIN_VALUE);

    byte[] bytes = unsignedShort.toByteArray();

    assertThat(bytes).isEqualTo(Ints.toByteArray(UnsignedShort.MIN_VALUE));
  }

  @Test
  public void toByteArray_valid_maxValue() {
    UnsignedShort unsignedShort = new UnsignedShort(UnsignedShort.MAX_VALUE);

    byte[] bytes = unsignedShort.toByteArray();

    assertThat(bytes).isEqualTo(Ints.toByteArray(UnsignedShort.MAX_VALUE));
  }
}
