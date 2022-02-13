package dev.sbutler.bitflask.resp.types;

import java.util.Objects;

public final class RespInteger extends RespType<Integer> {

  public static final char TYPE_PREFIX = ':';

  private final int value;

  public RespInteger(int value) {
    this.value = value;
  }

  @Override
  public Integer getValue() {
    return value;
  }

  @Override
  public byte[] getEncodedBytes() {
    byte[] encodedValueBytes = String.valueOf(value).getBytes(ENCODED_CHARSET);
    return getEncodedBytesFromValueBytes(encodedValueBytes, TYPE_PREFIX);
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RespInteger that = (RespInteger) o;
    return getValue().equals(that.getValue());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getValue());
  }
}
