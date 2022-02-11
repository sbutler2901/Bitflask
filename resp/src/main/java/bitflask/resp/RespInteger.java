package bitflask.resp;

public final class RespInteger extends RespType<Integer> {

  static final char TYPE_PREFIX = ':';

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
    byte[] encodedValueBytes = String.valueOf(value).getBytes(RespType.ENCODED_CHARSET);
    return RespType.getEncodedBytesFromValueBytes(encodedValueBytes, TYPE_PREFIX);
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }
}
