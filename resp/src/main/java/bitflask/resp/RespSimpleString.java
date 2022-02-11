package bitflask.resp;

public final class RespSimpleString extends RespType<String> {

  public static final char TYPE_PREFIX = '+';

  private final String value;

  public RespSimpleString(String value) {
    this.value = value;
  }

  @Override
  public String getValue() {
    return value;
  }

  @Override
  public byte[] getEncodedBytes() {
    byte[] encodedValueBytes = value.getBytes(RespType.ENCODED_CHARSET);
    return RespType.getEncodedBytesFromValueBytes(encodedValueBytes, TYPE_PREFIX);
  }

  @Override
  public String toString() {
    return value;
  }
}
