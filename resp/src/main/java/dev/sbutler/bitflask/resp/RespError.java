package dev.sbutler.bitflask.resp;

public final class RespError extends RespType<String> {

  static final char TYPE_PREFIX = '-';

  private final String value;

  public RespError(String value) {
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
}
