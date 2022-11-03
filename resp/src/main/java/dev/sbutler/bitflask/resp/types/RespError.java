package dev.sbutler.bitflask.resp.types;

import static com.google.common.base.Preconditions.checkNotNull;

public final class RespError extends RespElement {

  public static final char TYPE_PREFIX = '-';

  private final String value;

  public RespError(String value) {
    checkNotNull(value);
    this.value = value;
  }

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
}
