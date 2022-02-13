package dev.sbutler.bitflask.resp.types;

import java.util.Objects;

public final class RespSimpleString extends RespType<String> {

  public static final char TYPE_PREFIX = '+';

  private final String value;

  public RespSimpleString(String value) {
    if (value == null) {
      throw new IllegalArgumentException("Value must not be null");
    }
    this.value = value;
  }

  @Override
  public String getValue() {
    return value;
  }

  @Override
  public byte[] getEncodedBytes() {
    byte[] encodedValueBytes = value.getBytes(ENCODED_CHARSET);
    return getEncodedBytesFromValueBytes(encodedValueBytes, TYPE_PREFIX);
  }

  @Override
  public String toString() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RespSimpleString that = (RespSimpleString) o;
    return getValue().equals(that.getValue());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getValue());
  }
}
